# PairedAnimations

**Fabric client mod · Minecraft 1.21.11 · Java 21**

A project for paired Minecraft animations, cinematic cameras, and a graceful vanilla-compatible fallback.

## Current status: Stage 1 — local tango preview

The current prototype **is not yet a synchronized multiplayer dance**. It provides:

- Press **K** next to another player (up to 6 blocks away) to start a **16-second** two-person cinematic camera preview.
- A smoothly orbiting third-person camera that keeps both players in frame.
- Locally rendered tango-inspired arm, leg, head and torso poses on **both** participant models for viewers running the mod on that client.
- Press **K** again to abort. The original perspective is restored after the preview, on partner disconnect, or on world change.
- No automatic character movement, teleportation, server packets, or server modifications.

The preview runs **only on the machine that pressed K**. In particular, another player without this mod **does not** see the custom poses or a dance yet. A server-accepted movement choreography, an explicit invitation/consent system, and cross-client timing will be developed separately.

## How to test

1. Install Minecraft **1.21.11**, Fabric Loader and Fabric API.
2. Build with **JDK 21** and **Gradle 9.2.1**: `gradle build`. The repo has a GitHub Actions build as well.
3. Copy the mod JAR from `build/libs/` (not the sources JAR) to `.minecraft/mods`.
4. Join a world with another player, stand within 6 blocks, and press **K**.
5. Verify that both avatars remain visible and moving the camera **does not move your actual character**.

## Design goals

1. **Cutscene:** independent local camera (no player teleporting) and a fully editable choreography timeline.
2. **Enhanced rendering:** animate standard Steve/Alex body parts locally for clients that have the mod.
3. **Paired sessions:** invitations, partner acceptance, deterministic shared start time and cancellation.
4. **Vanilla spectators:** only server-approved walking/turning/crouching/hand-swing packets can be seen on unmodified clients. Custom limbs, local cutscenes and arbitrary avatar poses cannot be broadcast by a client-only mod.
5. **Robustness:** handle anti-cheat corrections, latency, interruption and world transitions without driving players through obstacles.

**Important:** Multiplayer invitation/sync will require an external communication channel or a server component; an ordinary Minecraft server does not relay custom client-only animation events for us. This repository does **not** contain such a service yet.

## Developer notes

Gradle Loom uses `net.fabricmc.fabric-loom-remap` because 1.21.11 is an obfuscated Minecraft version. Current stage intentionally avoids third-party animation libraries; it renders the vanilla model via a small mixin.

License: MIT.
