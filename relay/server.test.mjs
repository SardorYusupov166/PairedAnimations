import { test } from 'node:test';
import assert from 'node:assert/strict';
import WebSocket from 'ws';
import { createRelayServer } from './server.mjs';

const A = 'a1a1a1a1-a1a1-4a1a-8a1a-a1a1a1a1a1a1';
const B = 'b2b2b2b2-b2b2-4b2b-8b2b-b2b2b2b2b2b2';
const C = 'c3c3c3c3-c3c3-4c3c-8c3c-c3c3c3c3c3c3';

function onceMessage(ws, type, timeoutMs = 3500) {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => { ws.off('message', onMessage); reject(Error('Timeout waiting ' + type)); }, timeoutMs);
    const onMessage = raw => {
      const message = JSON.parse(raw.toString());
      if (message.type !== type) return;
      clearTimeout(timeout); ws.off('message', onMessage); resolve(message);
    };
    ws.on('message', onMessage);
  });
}
async function connect(port, uuid, scope='migosmc') {
  const ws = new WebSocket('ws://127.0.0.1:' + port);
  await new Promise((resolve, reject) => { ws.once('open', resolve); ws.once('error', reject); });
  const ready = onceMessage(ws, 'ready');
  ws.send(JSON.stringify({ type: 'hello', uuid, scope }));
  await ready;
  return ws;
}
async function closeServer(wss, sockets) {
  sockets.forEach(socket => socket.terminate());
  await new Promise(resolve => wss.close(resolve));
}

test('mutual invitation starts both clients at same time', async () => {
  const wss = createRelayServer({ port: 0 });
  await new Promise(resolve => wss.on('listening', resolve));
  const sockets = [];
  try {
    const port = wss.address().port;
    const a = await connect(port, A);
    const b = await connect(port, B);
    sockets.push(a,b);
    const invite = onceMessage(b, 'invite');
    a.send(JSON.stringify({ type: 'invite', to: B, dance: 'tango' }));
    const incoming = await invite;
    assert.equal(incoming.from, A);
    const startA = onceMessage(a, 'start');
    const startB = onceMessage(b, 'start');
    b.send(JSON.stringify({ type: 'accept', inviteId: incoming.inviteId }));
    const [sa, sb] = await Promise.all([startA, startB]);
    assert.equal(sa.startAt, sb.startAt);
    assert.equal(sa.leader, A);
    assert.equal(sa.follower, B);
    assert.ok(sa.startAt >= sa.serverNow + 2500);
    const cancelled = onceMessage(a, 'cancelled');
    b.send(JSON.stringify({ type: 'cancel' }));
    assert.equal((await cancelled).reason, 'cancelled');
  } finally {
    await closeServer(wss, sockets);
  }
});

test('registration is scoped per Minecraft server and wrong user cannot accept invitation', async () => {
  const wss = createRelayServer({ port: 0 });
  await new Promise(resolve => wss.on('listening', resolve));
  const sockets = [];
  try {
    const port = wss.address().port;
    const a = await connect(port,A);
    const b = await connect(port,B);
    const c = await connect(port,C,'other-server');
    sockets.push(a,b,c);
    const invitePromise = onceMessage(b, 'invite');
    a.send(JSON.stringify({ type: 'invite', to: B, dance: 'tango' }));
    const invited = await invitePromise;
    const invalid = onceMessage(c, 'error');
    c.send(JSON.stringify({ type: 'accept', inviteId: invited.inviteId }));
    assert.equal((await invalid).reason, 'invite_not_found');
    const declined = onceMessage(a, 'declined');
    b.send(JSON.stringify({ type: 'decline', inviteId: invited.inviteId }));
    assert.equal((await declined).reason, 'declined');
  } finally {
    await closeServer(wss, sockets);
  }
});
