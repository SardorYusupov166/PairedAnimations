import { randomUUID } from 'node:crypto';
import { WebSocketServer, WebSocket } from 'ws';

const ID = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
const ROOM_TIMEOUT_MS = 20_000;
const MIN_INVITE_INTERVAL_MS = 3_000;

export function createRelayServer({ host = '127.0.0.1', port = 8765 } = {}) {
  const wss = new WebSocketServer({ host, port, maxPayload: 2048, clientTracking: true });
  const peers = new Map();
  const invitations = new Map();
  const active = new Map();

  const identityKey = (scope, uuid) => scope + ':' + uuid;
  const send = (socket, packet) => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(packet));
    }
  };
  const error = (socket, reason) => send(socket, { type: 'error', reason });
  const validId = value => typeof value === 'string' && ID.test(value);
  const validScope = value => typeof value === 'string' && value.length > 0 && value.length <= 160 && /^[a-zA-Z0-9.:_\-]+$/.test(value);
  const clearSession = (socket, reason) => {
    const partner = active.get(socket);
    active.delete(socket);
    if (partner) {
      active.delete(partner);
      send(partner, { type: 'cancelled', reason });
    }
  };
  const clearInvites = socket => {
    for (const [id, pending] of invitations) {
      if (pending.from === socket || pending.to === socket) {
        invitations.delete(id);
        const other = pending.from === socket ? pending.to : pending.from;
        send(other, { type: 'declined', inviteId: id, reason: 'disconnected' });
      }
    }
  };

  wss.on('connection', socket => {
    socket.identity = null;
    socket.lastInvite = 0;
    socket.on('message', raw => {
      let data;
      try { data = JSON.parse(raw.toString()); } catch { error(socket, 'invalid_json'); return; }
      if (!data || typeof data !== 'object' || Array.isArray(data) || typeof data.type !== 'string') {
        error(socket, 'invalid_packet');
        return;
      }
      if (data.type === 'hello') {
        if (socket.identity || !validId(data.uuid) || !validScope(data.scope)) {
          error(socket, 'invalid_registration');
          return;
        }
        const uuid = data.uuid.toLowerCase();
        const key = identityKey(data.scope, uuid);
        if (peers.has(key)) {
          error(socket, 'already_registered');
          socket.close(1008, 'duplicate identity');
          return;
        }
        socket.identity = { uuid, scope: data.scope, key };
        peers.set(key, socket);
        send(socket, { type: 'ready', serverTime: Date.now() });
        return;
      }
      if (!socket.identity) { error(socket, 'not_registered'); return; }

      if (data.type === 'invite') {
        if (!validId(data.to) || data.dance !== 'tango') { error(socket, 'invalid_invite'); return; }
        if (Date.now() - socket.lastInvite < MIN_INVITE_INTERVAL_MS) { error(socket, 'invite_rate_limit'); return; }
        socket.lastInvite = Date.now();
        const recipient = peers.get(identityKey(socket.identity.scope, data.to.toLowerCase()));
        if (!recipient || recipient === socket || active.has(socket) || active.has(recipient)) {
          error(socket, 'partner_unavailable');
          return;
        }
        const id = randomUUID();
        invitations.set(id, { from: socket, to: recipient, expiresAt: Date.now() + ROOM_TIMEOUT_MS });
        send(recipient, { type: 'invite', inviteId: id, from: socket.identity.uuid, dance: data.dance });
        send(socket, { type: 'invite_sent', inviteId: id, to: recipient.identity.uuid });
        return;
      }

      if (data.type === 'accept' || data.type === 'decline') {
        if (!validId(data.inviteId)) { error(socket, 'invalid_invite_id'); return; }
        const pending = invitations.get(data.inviteId);
        if (!pending || pending.to !== socket) { error(socket, 'invite_not_found'); return; }
        invitations.delete(data.inviteId);
        if (pending.expiresAt < Date.now()) {
          error(socket, 'invite_expired');
          send(pending.from, { type: 'declined', inviteId: data.inviteId, reason: 'expired' });
          return;
        }
        if (data.type === 'decline') {
          send(pending.from, { type: 'declined', inviteId: data.inviteId, reason: 'declined' });
          return;
        }
        if (!pending.from.identity || !socket.identity || active.has(pending.from) || active.has(socket)) {
          error(socket, 'partner_unavailable');
          return;
        }
        active.set(pending.from, socket);
        active.set(socket, pending.from);
        const serverNow = Date.now();
        const startAt = serverNow + 3_000;
        const common = {
          type: 'start',
          inviteId: data.inviteId,
          leader: pending.from.identity.uuid,
          follower: socket.identity.uuid,
          startAt,
          serverNow
        };
        send(pending.from, common);
        send(socket, common);
        return;
      }

      if (data.type === 'cancel') {
        clearSession(socket, 'cancelled');
        return;
      }
      if (data.type === 'ping') {
        send(socket, { type: 'pong', serverTime: Date.now() });
        return;
      }
      error(socket, 'unknown_packet');
    });

    socket.on('close', () => {
      if (socket.identity && peers.get(socket.identity.key) === socket) peers.delete(socket.identity.key);
      clearInvites(socket);
      clearSession(socket, 'disconnected');
    });
    socket.on('error', () => {});
  });

  const cleanup = setInterval(() => {
    for (const [id, pending] of invitations) {
      if (pending.expiresAt <= Date.now()) {
        invitations.delete(id);
        send(pending.to, { type: 'expired', inviteId: id });
        send(pending.from, { type: 'declined', inviteId: id, reason: 'expired' });
      }
    }
  }, 1_000);
  cleanup.unref();

  wss.on('close', () => clearInterval(cleanup));
  return wss;
}

if (process.argv[1] && import.meta.url === new URL('file://' + process.argv[1]).href) {
  const port = Number(process.env.PAIRED_RELAY_PORT || 8765);
  const host = process.env.PAIRED_RELAY_HOST || '127.0.0.1';
  createRelayServer({ host, port }).on('listening', () => {
    console.log('PairedAnimations relay listening on ' + host + ':' + port);
    console.log('Do not expose plain ws:// to the public Internet. Use a TLS proxy (wss://).');
  });
}
