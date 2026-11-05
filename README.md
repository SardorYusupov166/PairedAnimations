# PairedAnimations — Minecraft **26.2** (Fabric)

**Current build: 0.2.0-alpha.1 — local cinematic preview.**

PairedAnimations is a client-side project for multiplayer paired dances using vanilla player models, with local cinematic cameras and richer limb animation on modded clients.

## Requirements

- Minecraft **26.2** (not 1.21.11).
- Fabric Loader **0.19.5+** and Fabric API for **26.2**.
- **Java 25**.
- To build: Gradle **9.7.1**, or download the successful **PairedAnimations-26.2-alpha** artifact from GitHub Actions.

## Prototype controls

1. Join a world with another player and stand within **6 blocks**.
2. Press **K** to launch a **16-second** orbit-camera preview containing both players.
3. Press **K** again to stop. Camera perspective is restored at the end, when the other player disappears, or on world change.

The preview applies tango-inspired arm, leg, torso, and head poses to the two player models **rendered by your own client**. It does **not** animate actual movement, synchronize the other client's camera, send dance packets, or show custom poses to vanilla spectators yet. This remains a local test, not the finished tango.

## Roadmap

1. Complete 26.2 runtime camera/model testing (including classic/SLIM model and nearby geometry).
2. Partner invitations, consent and external session sync (no modifications to the target Minecraft server).
3. Same choreography timeline and cinematic cameras on both modded clients.
4. Vanilla-visible regular Minecraft walking/turning/crouching movements, validated against the server's movement rules.
5. Additional dances and reusable choreographic editor.

### Developer build

Run `gradle build` using **JDK 25** and Gradle 9.7.1. The CI workflow runs the exact same build. Install the generated main JAR from `build/libs` (not the sources JAR) in your Fabric 26.2 client alongside Fabric API.

This branch replaces the 1.21.11 target. The original prototype is preserved on `feature/cinematic-tango-prototype`.

## Technical notes

Minecraft 26.2 ships with unobfuscated Mojang names. This project uses `net.fabricmc.fabric-loom` (not `fabric-loom-remap`) and intentionally has no `mappings` declaration. The camera and model hooks avoid raw OpenGL to stay compatible with the optional Vulkan backend.

License: MIT.
