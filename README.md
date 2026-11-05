# PairedAnimations — Minecraft 26.2 (Fabric)

**Current build: 0.2.0-alpha.2 — local cinematic preview.**

Client-side paired-animations prototype based on the vanilla Steve/Alex body model. The previous `0.2.0-alpha.1` has a **startup crash on 26.2** because its camera mixin targeted the removed `Camera.setup` method. **Alpha.2 fixes this by injecting into `Camera.update(DeltaTracker)`**.

## Requirements

- Minecraft **26.2** (not 1.21.11)
- Fabric Loader **0.19.5+**, Fabric API for Minecraft **26.2**
- Java **25**

## How to test

1. Install the new `PairedAnimations-26.2-alpha` artifact from a successful build in GitHub Actions, using only the main JAR, not the sources JAR.
2. Launch Minecraft 26.2. If it opens the title screen without a `pairedanimations.mixins.json:CameraMixin` error, the startup regression is resolved.
3. Stand within **6 blocks** of another player in the same world.
4. Press **K** to start the **16-second** orbit-camera preview. Press **K** again to stop.
5. Check that both avatars are visible, your perspective is restored afterward, and the other player's camera does not unexpectedly change.

The preview applies tango-inspired arm, leg, torso, and head poses to the two player models **rendered locally**. It does not yet synchronize the second client or transmit custom animation to vanilla spectators, nor does it move actual player positions.

## Roadmap

1. Runtime tests on 26.2, especially camera frustum/collision and Steve/SLIM skins.
2. Partner invitations with consent and an external timing/sync channel.
3. A shared choreography timeline and local cinematic cameras on both clients.
4. Vanilla-visible legal walking/turning/crouching movement confirmed by the game server.
5. More dances and a choreography editor.

## Building

Requires Java 25 and Gradle 9.7.1. Run `gradle build`, or use GitHub Actions build artifacts.

The original 1.21.11 source remains in `feature/cinematic-tango-prototype`.

License: MIT.
