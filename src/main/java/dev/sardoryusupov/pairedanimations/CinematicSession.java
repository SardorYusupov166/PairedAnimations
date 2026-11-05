package dev.sardoryusupov.pairedanimations;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Camera and vanilla-body animation timeline on a single client. Does not
 * physically move players or send packets to the Minecraft game server.
 */
public final class CinematicSession {
    private UUID localPlayerId;
    private UUID partnerId;
    private ClientLevel originLevel;
    private CameraType previousCamera;
    private long startNanos;
    private boolean cameraSwitched;
    private boolean localIsLeader;

    public boolean isActive() {
        return partnerId != null;
    }

    /** Legacy solo preview: animates both visible players on this client only. */
    public void start(Minecraft client, Player partner) {
        schedule(client, partner, true, 0);
    }

    /** Both modded clients schedule the same choreography time independently. */
    public void schedule(Minecraft client, Player partner, boolean localIsLeader, long delayMillis) {
        if (client.player == null || client.level == null) return;
        stop(client);
        localPlayerId = client.player.getUUID();
        partnerId = partner.getUUID();
        originLevel = client.level;
        previousCamera = client.options.getCameraType();
        this.localIsLeader = localIsLeader;
        cameraSwitched = false;
        startNanos = System.nanoTime() + Math.max(0, Math.min(delayMillis, 10_000)) * 1_000_000L;
    }

    public void tick(Minecraft client) {
        if (!isActive()) return;
        Player partner = findPartner(client);
        if (client.level != originLevel || client.player == null
            || !client.player.getUUID().equals(localPlayerId)
            || !client.player.isAlive() || partner == null || !partner.isAlive()
            || client.player.distanceToSqr(partner) > 16 * 16
            || elapsedTicks() >= TangoTimeline.DURATION_TICKS) {
            stop(client);
            return;
        }
        if (!cameraSwitched && System.nanoTime() >= startNanos) {
            cameraSwitched = true;
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    public void stop(Minecraft client) {
        if (!isActive()) return;
        partnerId = null;
        localPlayerId = null;
        originLevel = null;
        startNanos = 0;
        cameraSwitched = false;
        if (previousCamera != null) {
            client.options.setCameraType(previousCamera);
            previousCamera = null;
        }
    }

    private Player findPartner(Minecraft client) {
        if (client.level == null || partnerId == null) return null;
        for (Player player : client.level.players()) {
            if (player.getUUID().equals(partnerId)) return player;
        }
        return null;
    }

    private float elapsedTicks() {
        return Math.max(0f, (System.nanoTime() - startNanos) / 50_000_000f);
    }

    public CameraShot cameraShot(Minecraft client, float partialTick) {
        if (!isActive() || !cameraSwitched || client.player == null || client.level != originLevel) return null;
        Player partner = findPartner(client);
        if (partner == null) return null;

        float time = elapsedTicks();
        float progress = Math.min(1f, time / TangoTimeline.DURATION_TICKS);
        Vec3 focus = client.player.position().add(partner.position()).scale(0.5).add(0, 1.15, 0);

        double theta = 0.9 + progress * Math.PI * 2.0;
        double radius = 5.2 - 0.9 * Math.sin(progress * Math.PI);
        double elevation = 2.0 + 0.5 * Math.sin(progress * Math.PI * 2.0);
        Vec3 camera = focus.add(Math.cos(theta) * radius, elevation, Math.sin(theta) * radius);
        Vec3 direction = focus.subtract(camera);
        double horizontal = Math.hypot(direction.x, direction.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, horizontal));

        return new CameraShot(camera, yaw, pitch);
    }

    public TangoTimeline.PoseFrame poseFor(int entityId, Minecraft client) {
        if (!isActive() || !cameraSwitched || client.level != originLevel || client.player == null) return null;
        boolean isLocal = client.player.getId() == entityId;
        if (!isLocal) {
            Player partner = findPartner(client);
            if (partner == null || partner.getId() != entityId) return null;
        }
        return TangoTimeline.poseAt(elapsedTicks(), isLocal ? localIsLeader : !localIsLeader);
    }

    public record CameraShot(Vec3 position, float yaw, float pitch) {}
}
