# PairedAnimations — Minecraft 26.2 / Fabric

**Experimental build 0.5.0-alpha.1** · Java 25 · Fabric Loader 0.19.5+ · Fabric API for Minecraft 26.2.

PairedAnimations is a client-side experiment for paired tango: real conservative vanilla-visible steps, locally animated Steve/Alex limbs, and a two-player cinematic camera.

## NEW — dance pairing directly through MigosMC private messages

**No external server, Node.js, VPN or additional Minecraft server plugin is needed for chat-based invitations.**

On MigosMC the tested PM display is `[~Player -> я] text`. PairedAnimations now recognizes **strictly formatted** private-message protocol packets inside this server-generated wrapper. It sends only a few `/msg` commands to establish one pair and start the existing deterministic 16-second choreography; it never sends steps individually over chat.

### First test

1. Both participants install **the same 0.5.0-alpha.1 JAR** in Minecraft 26.2 with Fabric API.
2. Join MigosMC and stand 2–4 blocks apart on a safe flat platform.
3. Player A presses **K**. The mod sends `/msg <partner> PA1:I:...`.
4. Player B sees `[PairedAnimations] ... приглашает тебя на танго` and presses **J** (or **N** to decline).
5. A 6-second countdown is scheduled using a PM handshake. Both players then execute the same footwork and local camera animation; **K** cancels.
6. Nearby unmodified players should see ordinary steps/turns carried by the Minecraft server. They cannot see the custom limb pose.

The protocol packets deliberately **remain visible in chat in this alpha** to make troubleshooting with ChatSorter and unusual server-side formatting possible. In a future release these can be hidden after MigosMC compatibility is confirmed.

If no invitation is detected after K, look for the `PA1:I:...` packet in the recipient's private messages and report the precise line in chat. This will help tune the parser for the server's PM plugin. If the server filters the text or blocks scripted `/msg` commands, chat sync may not work there.

**Timing limitation:** the START message contains a future timestamp derived from the inviter's PC clock. Please enable Windows automatic time synchronization on both PCs. Large clock differences abort pairing. Chat delays/antispam may still cause partial desync.

**Safety:** Never start dances near drops, water or obstacles. The mod drives only your own normal movement-key bindings; vanilla Minecraft server physics and anticheat remain authoritative. Press K to cancel; normal camera and synthetic controls are restored. If the partner disconnects or a menu opens, movement stops.

### Optional WebSocket relay

The old relay is still supported as an **optional transport**. If a configured WebSocket relay is connected, K/J/N use it; otherwise **chat PMs are the default**. The private prototype relay is not authenticated; see [relay/README.md](relay/README.md) before exposing a relay externally.

### What is not ready

- No end-to-end two-PC in-game validation on MigosMC yet (only Java, relay, and Minecraft title-screen CI checks).
- Chat messages aren't broadcast to bystanders; viewers with the mod but not involved in the dance won't automatically get enhanced poses in chat-only mode. Standard Minecraft motion remains visible to everyone.
- Tangos remain a first short prototype: not advanced ballroom choreography, custom hand holding, or a keyframe editor.
- No anti-spam bypass; chat pairing depends on server permission to use `/msg` and on its PM formatting.

### Build and CI

Use JDK 25, Gradle 9.7.1 and `gradle build`. GitHub Actions uploads the main mod JAR under **PairedAnimations-26.2-alpha**; do not use the sources JAR.

The older 1.21.11 code is preserved on `feature/cinematic-tango-prototype`.

License: MIT.
