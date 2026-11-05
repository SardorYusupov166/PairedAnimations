package dev.sardoryusupov.pairedanimations;

/**
 * A deterministic, testable demo animation for the stock vanilla player model.
 * This does not move the real players. Angles are in radians.
 */
public final class TangoTimeline {
    public static final int DURATION_TICKS = 320;

    private TangoTimeline() {}

    public static PoseFrame poseAt(float tick, boolean leader) {
        float t = clamp(tick, 0, DURATION_TICKS);
        float fadeIn = smoothstep(0, 35, t);
        float fadeOut = 1 - smoothstep(286, DURATION_TICKS, t);
        float strength = fadeIn * fadeOut;

        float side = leader ? 1f : -1f;
        float sway = (float) Math.sin(t * 0.065f);
        float walk = (float) Math.sin(t * 0.135f);
        float turn = smoothstep(105, 155, t) * (1 - smoothstep(205, 245, t));
        float finale = smoothstep(248, 285, t);

        float leftArmX = strength * (-0.65f + 0.10f * sway);
        float rightArmX = strength * (-0.95f - 0.10f * sway);
        float leftArmY = strength * (-side * 0.24f);
        float rightArmY = strength * (side * 0.28f);
        float leftArmZ = strength * (-0.22f + side * 0.08f * sway);
        float rightArmZ = strength * (0.34f + side * 0.08f * sway);

        float leftLegX = strength * (0.29f * walk * side);
        float rightLegX = -leftLegX;
        float bodyX = strength * (0.06f + 0.1f * finale);
        float bodyY = strength * (side * 0.08f * sway + side * 0.13f * turn);
        float bodyZ = strength * (side * 0.05f * sway + side * 0.13f * finale);
        float headX = strength * (-0.07f + 0.05f * sway);

        return new PoseFrame(leftArmX, rightArmX, leftArmY, rightArmY,
            leftArmZ, rightArmZ, leftLegX, rightLegX, bodyX, bodyY, bodyZ, headX);
    }

    private static float clamp(float value, float lo, float hi) {
        return Math.max(lo, Math.min(hi, value));
    }

    private static float smoothstep(float start, float end, float value) {
        float x = clamp((value - start) / (end - start), 0, 1);
        return x * x * (3 - 2 * x);
    }

    public record PoseFrame(
        float leftArmX, float rightArmX,
        float leftArmY, float rightArmY,
        float leftArmZ, float rightArmZ,
        float leftLegX, float rightLegX,
        float bodyX, float bodyY, float bodyZ, float headX
    ) {}
}
