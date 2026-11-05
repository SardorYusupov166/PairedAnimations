package dev.sardoryusupov.pairedanimations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Renders full limb/torso animations for nearby third-party viewers with the
 * mod installed. Never changes their camera or game-world player positions.
 */
public final class SpectatorAnimations {
    private final Map<String, Session> sessions = new HashMap<>();
    private ClientLevel world;

    public void start(Minecraft client, UUID leader, UUID follower, long delayMillis) {
        if (client.level == null) return;
        if (client.player != null
            && (client.player.getUUID().equals(leader) || client.player.getUUID().equals(follower))) {
            return;
        }
        if (world != client.level) {
            sessions.clear();
            world = client.level;
        }
        // Guard against repeated events and very long clock offsets.
        if (sessions.size() >= 12) sessions.clear();
        long startsAt = System.nanoTime() + Math.max(0, Math.min(10_000, delayMillis)) * 1_000_000L;
        sessions.put(key(leader, follower), new Session(leader, follower, startsAt));
    }

    public void cancel(UUID leader, UUID follower) {
        sessions.remove(key(leader, follower));
    }

    public void tick(Minecraft client) {
        if (client.level != world || client.player == null) {
            sessions.clear();
            world = client.level;
        }
        Iterator<Session> it = sessions.values().iterator();
        long now = System.nanoTime();
        while (it.hasNext()) {
            Session session = it.next();
            if (now - session.startsAt > TangoTimeline.DURATION_TICKS * 50_000_000L) it.remove();
        }
    }

    public TangoTimeline.PoseFrame poseFor(int entityId, Minecraft client) {
        if (client.level == null || world != client.level) return null;
        UUID targetId = null;
        for (Player player : client.level.players()) {
            if (player.getId() == entityId) {
                targetId = player.getUUID();
                break;
            }
        }
        if (targetId == null) return null;
        long now = System.nanoTime();
        for (Session session : sessions.values()) {
            if (now < session.startsAt) continue;
            float tick = (now - session.startsAt) / 50_000_000f;
            if (tick >= TangoTimeline.DURATION_TICKS) continue;
            if (targetId.equals(session.leader)) return TangoTimeline.poseAt(tick, true);
            if (targetId.equals(session.follower)) return TangoTimeline.poseAt(tick, false);
        }
        return null;
    }

    private static String key(UUID leader, UUID follower) {
        return leader.toString() + ":" + follower;
    }

    private record Session(UUID leader, UUID follower, long startsAt) {}
}
