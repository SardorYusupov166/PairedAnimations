package dev.sardoryusupov.pairedanimations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Simulates ordinary keyboard walking rather than teleporting the player
 * or sending handcrafted packets. Only this client's own player is driven.
 * The Minecraft server remains authoritative over movement and collision.
 */
public final class TangoMovement {
    private static final double TARGET_TOLERANCE = 0.095;
    private static final double MAX_DISTANCE_FROM_ORIGIN = 1.65;
    private static final double MAX_CORRECTION = 0.95;

    private boolean active;
    private Vec3 origin;
    private Vec3 forward;
    private Vec3 sideways;
    private double forwardScale;
    private Vec3 previousPosition;
    private int blockedTicks;

    public boolean tick(Minecraft client, Player partner, boolean leader, float tick) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || partner == null || client.gui.screen() != null) {
            stop(client);
            return false;
        }
        if (!player.isAlive() || !player.onGround() || player.isPassenger()
            || player.isInWater() || player.isInLava()) {
            stop(client);
            return false;
        }
        if (!active && !begin(client, partner, leader)) return false;

        double distanceToPartner = player.distanceTo(partner);
        if (distanceToPartner < 1.25 || distanceToPartner > 12) {
            stop(client);
            return false;
        }
        Vec3 current = player.position();
        if (current.distanceToSqr(origin) > MAX_DISTANCE_FROM_ORIGIN * MAX_DISTANCE_FROM_ORIGIN) {
            stop(client);
            return false;
        }
        TangoFootwork.Offset step = TangoFootwork.at(tick);
        Vec3 target = origin
            .add(forward.scale(step.forward() * forwardScale))
            .add(sideways.scale(step.side()));
        Vec3 diff = target.subtract(current);
        double difference = Math.hypot(diff.x, diff.z);

        // Abort if the ground beneath the intended next footstep disappears.
        Vec3 next = difference > 0.001
            ? current.add(diff.x / difference * Math.min(0.17, difference), 0,
                          diff.z / difference * Math.min(0.17, difference))
            : current;
        BlockPos floor = BlockPos.containing(next.x, player.getY() - 0.28, next.z);
        if (client.level.getBlockState(floor).isAir()) {
            stop(client);
            return false;
        }
        if (difference > MAX_CORRECTION) {
            stop(client);
            return false;
        }

        // Always keep facing the partner, while the real character walks,
        // strafes, or steps backward as tango requires.
        Vec3 facing = partner.position().subtract(current);
        if (facing.horizontalDistanceSqr() > 0.001) {
            float desiredYaw = (float) Math.toDegrees(Math.atan2(-facing.x, facing.z));
            float deltaYaw = wrapDegrees(desiredYaw - player.getYRot());
            float yaw = player.getYRot() + Math.max(-7f, Math.min(7f, deltaYaw));
            player.setYRot(yaw);
            player.setYHeadRot(yaw);
            player.setYBodyRot(yaw);
        }

        boolean up = false, down = false, left = false, right = false;
        if (difference > TARGET_TOLERANCE) {
            float yawRad = (float) Math.toRadians(player.getYRot());
            // Minecraft yaw: 0 = +Z; 90 = -X.
            double forwardAmount = -Math.sin(yawRad) * diff.x + Math.cos(yawRad) * diff.z;
            double rightAmount = -Math.cos(yawRad) * diff.x - Math.sin(yawRad) * diff.z;

            // One input axis at a time prevents sprint-speed diagonal motion.
            if (Math.abs(forwardAmount) >= Math.abs(rightAmount)) {
                up = forwardAmount > 0;
                down = forwardAmount < 0;
            } else {
                right = rightAmount > 0;
                left = rightAmount < 0;
            }

            if (previousPosition != null && previousPosition.distanceToSqr(current) < 0.000001) {
                blockedTicks++;
            } else {
                blockedTicks = 0;
            }
            if (blockedTicks >= 20) {
                stop(client);
                return false;
            }
        } else {
            blockedTicks = 0;
        }
        previousPosition = current;
        applyKeys(client, up, down, left, right);
        return true;
    }

    private boolean begin(Minecraft client, Player partner, boolean leader) {
        LocalPlayer player = client.player;
        if (player == null) return false;
        // Never override controls that the user is already holding.
        if (client.options.keyUp.isDown() || client.options.keyDown.isDown()
            || client.options.keyLeft.isDown() || client.options.keyRight.isDown()) {
            inform(player, "Отпусти клавиши движения перед началом танца.");
            return false;
        }
        origin = player.position();
        Vec3 towardFollower = leader
            ? partner.position().subtract(origin)
            : origin.subtract(partner.position());
        double length = towardFollower.horizontalDistance();
        if (length < 1.45) {
            inform(player, "Нужно чуть больше пространства между партнёрами.");
            return false;
        }
        forward = new Vec3(towardFollower.x / length, 0, towardFollower.z / length);
        sideways = new Vec3(-forward.z, 0, forward.x);
        forwardScale = Math.min(1.0, Math.max(0.0, (length - 1.40) / 0.42));
        blockedTicks = 0;
        previousPosition = origin;
        active = true;
        return true;
    }

    private static float wrapDegrees(float degrees) {
        degrees %= 360f;
        if (degrees >= 180) degrees -= 360f;
        if (degrees < -180) degrees += 360f;
        return degrees;
    }

    private static void inform(LocalPlayer player, String message) {
        player.sendSystemMessage(Component.literal("[PairedAnimations] " + message));
    }

    private static void applyKeys(Minecraft client, boolean up, boolean down, boolean left, boolean right) {
        client.options.keyUp.setDown(up);
        client.options.keyDown.setDown(down);
        client.options.keyLeft.setDown(left);
        client.options.keyRight.setDown(right);
    }

    public void stop(Minecraft client) {
        if (active) applyKeys(client, false, false, false, false);
        active = false;
        origin = null;
        previousPosition = null;
        forward = null;
        sideways = null;
        blockedTicks = 0;
    }
}
