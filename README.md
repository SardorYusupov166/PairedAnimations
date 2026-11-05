# PairedAnimations — Minecraft 26.2 / Fabric

**Experimental build 0.4.0-alpha.1** · Java 25 · Fabric Loader 0.19.5+ · Fabric API for 26.2.

PairedAnimations is an experimental paired tango mod. This version adds **real, small walking/turning steps** to the existing 16-second synchronized client-side animation and cinematic camera.

## What changed in 0.4.0-alpha.1

- **Real vanilla player movement:** the mod presses ordinary movement key bindings for its own local player. The Minecraft server receives normal movement updates, so nearby unmodified clients can see walking and turning.
- A conservative 16-second footwork sequence: approach, retreat, side step, counter-step, finish. Maximum offset from the start is under one block.
- The local player turns toward the partner during the steps. The cinematic camera remains client-only.
- With a connected relay and accepted invitation, **both participants perform matching world-space steps** from the shared start time. Without a relay, K runs a **one-person motion preview**; the partner is stationary on their own client.
- Safety stops movement and restores camera on K, on world/partner disconnect, when a GUI opens, when leaving ground, when moving near an edge, when blocked, and on large server position corrections. It releases any synthetic movement keys on stop.

## Important limits

- This is still a **prototype tango** rather than an advanced realistic duet. Viewers without the mod see only real vanilla steps and turns, **not** custom arm/leg poses. Modded viewers attached to the same relay see detailed limb poses.
- Only the participant's own mod controls their character; no modded client can force a nearby unmodded player to dance.
- Minecraft-server movement/anti-cheat rules remain authoritative. Test on flat solid ground in a safe area with at least two blocks of spacing, and no items / mobs / edges close to the players.
- Movement keys are temporarily controlled by the mod during the scene. Press **K** to cancel. For best results release WASD before starting. Do not start in water, midair or while riding.
- The external relay prototype is not account-authenticated. Only use a private trusted relay.

## How to test

1. Install the main JAR from GitHub Actions, Fabric Loader and Fabric API for **Minecraft 26.2**, Java 25.
2. Stand near another player on a wide, flat safe surface. Leave 2–4 blocks between you.
3. Without relay: press **K** to test **your own character's** actual steps; the other player stays still.
4. With both clients connected to the **same trusted relay**: inviter presses **K**; partner accepts with **J**; 3-second countdown then both players perform the steps. **N** declines.
5. Observe from an ordinary (unmodded) Minecraft client: walking and rotation should be visible, while custom poses will not.
6. Stop anytime with **K**; verify that the camera and movement keys return to normal.

### Relay configuration

The Minecraft server needs **no plugin**. To pair players on different PCs, run the included `relay/` Node.js service (Node 20+) on a trusted separate host, and write the same relay `wss://...` URL to `config/pairedanimations-relay.txt` on each client. Restart Minecraft. Refer to [relay/README.md](relay/README.md) for security constraints.

### Build and CI

Java 25 · Gradle 9.7.1: `gradle build`. CI also runs relay tests and a graphical client startup smoke test. A green CI build does **not** replace manual in-world verification of tango movement and sync.

The original 1.21.11 code is preserved in `feature/cinematic-tango-prototype`.

License: MIT.
