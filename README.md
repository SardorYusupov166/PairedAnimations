# PairedAnimations — Minecraft 26.2 / Fabric

**Experimental build: 0.3.0-alpha.1.** Java 25 · Fabric Loader 0.19.5+ · Fabric API for 26.2.

An experimental paired dance mod with a 16-second tango-inspired vanilla-player pose animation and a two-player cinematic camera.

## What works

- **K** near another player (within 6 blocks): start a local cinematic preview when no external relay is configured.
- When a relay is configured and connected, **K** sends a tango invitation to the nearest player (both need the mod).
- **J** accepts an incoming invitation; **N** declines it.
- After acceptance, both clients are given the same start timestamp, a three-second countdown, local rotating cinematic cameras, and complementary limb/torso/head poses.
- Third-party viewers who also have PairedAnimations installed and are connected to the same relay see the two dancers' **full local pose animations** without camera changes.
- **K** during a scene cancels and restores the previous camera. The scene also stops if the partner leaves or the dance ends.
- A local Minecraft 26.2 instance starts without the earlier CameraMixin crash (startup smoke test on 0.2.0-alpha.2).

## What is NOT implemented yet

- **No automatically choreographed real walking/turning/crouching packets** have been added. Players without the mod **do not** see a real synchronized dance, just the ordinary game avatars and their actual actions.
- This first tango is a test pose sequence, not motion-captured choreography. Character positions do not move.
- End-to-end two-machine in-game tests (camera perspectives, custom model poses, concurrent movement) are not yet complete.
- Relay uses a private, unauthenticated prototype protocol. The server trusts the self-declared UUID; do not expose a public relay without proper identity validation and anti-abuse controls.

## Enabling invitations with an external relay

The Minecraft server itself does **not** need a plugin, but the two modded clients **do need to reach the same separate WebSocket relay**.

1. Install the client JAR, Fabric Loader and Fabric API for Minecraft 26.2 on both PCs.
2. On one trusted machine, start the included `relay/` service using **Node.js 20+**:

   ```bash
   cd relay
   npm install
   npm test
   npm start
   ```

3. Set `config/pairedanimations-relay.txt` on each Minecraft client to the SAME URL on a line by itself. For a same-computer test, use `ws://127.0.0.1:8765`. For different PCs, deploy the relay behind TLS and use `wss://your-relay-domain` (requires hosting/network setup).
4. Restart Minecraft after editing the config file. In chat, each client should report a successful relay connection.
5. Both players join the **same Minecraft server address**, stand within 6 blocks, and use **K → J**.

The relay transmits only invitations, accept/decline, start/cancel and clock synchronization. It does not change Minecraft-server settings or bypass anti-cheat. See [relay/README.md](relay/README.md) for operational and security limits.

## Build and CI

- Requires JDK 25, Gradle 9.7.1: `gradle build`
- Node.js 20+ for relay tests: `cd relay && npm install && npm test`
- GitHub Actions builds the JAR, tests the relay, and runs a Minecraft title-screen startup smoke test on a virtual display.

Download the main JAR (not `-sources.jar`) from the successful **Build Fabric mod** workflow artifact `PairedAnimations-26.2-alpha`.

## Next development milestones

1. Validate both clients in-game and harden relay account identity/hosted service setup.
2. Animate **actual movement** within the rules of the Minecraft server, so vanilla spectators can see the choreography.
3. Synchronize a richer keyframed tango, camera transitions, collision checks and player movement cancellation.
4. Add a dance selection menu and choreography editor.

The previous source for Minecraft 1.21.11 remains available in `feature/cinematic-tango-prototype`.

License: MIT.
