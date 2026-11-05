# PairedAnimations invitation relay (experimental)

An optional, lightweight WebSocket relay **outside Minecraft**. No Minecraft server-side mod or plugin is required. The relay only passes invite, accept, decline, start and cancel messages; it cannot move players.

## Start

Requires Node.js 20+.

```bash
cd relay
npm install
npm test
npm start
```

By default binds to **127.0.0.1:8765**. To serve two PCs across the internet, deploy on a trusted machine and provide **HTTPS/TLS via a WSS reverse proxy**. Both mod clients must set the same `wss://` URL in `config/pairedanimations-relay.txt`. For loopback testing, `ws://127.0.0.1:8765` is allowed.

Use `PAIRED_RELAY_HOST` and `PAIRED_RELAY_PORT` to adjust bind address and port. Do not publish `ws://` on the Internet.

**Security limits:** This is a trusted-private-test relay without Mojang account authentication. It accepts self-reported UUIDs and world scopes. Do not use as a general public multiplayer service; a malicious client could claim another person's identity. Player proximity and explicit consent must be checked by the client. There is a 3s invite rate limit, 20s invite expiry and a 2KB packet limit.

The currently planned session communicates a 3-second synchronized countdown; the clients must implement their own camera and model poses. Vanilla spectators can only see normal movements carried by the Minecraft server, not these custom animations.
