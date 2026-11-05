package dev.sardoryusupov.pairedanimations;

/**
 * Small, deliberately conservative vanilla-compatible dance steps.
 * Offsets are in blocks relative to the initial leader->follower direction.
 * Both participants perform identical world-space translations, so their
 * original separation is maintained if both clients are connected.
 */
public final class TangoFootwork {
    private TangoFootwork() {}

    public static Offset at(float tick) {
        float t = Math.max(0f, Math.min(TangoTimeline.DURATION_TICKS, tick));
        if (t < 25) return new Offset(0, 0);
        if (t < 70) return blend(t, 25, 70, 0, 0, 0.42, 0);
        if (t < 92) return new Offset(0.42, 0);
        if (t < 132) return blend(t, 92, 132, 0.42, 0, -0.18, 0);
        if (t < 155) return new Offset(-0.18, 0);
        if (t < 200) return blend(t, 155, 200, -0.18, 0, -0.18, 0.42);
        if (t < 220) return new Offset(-0.18, 0.42);
        if (t < 266) return blend(t, 220, 266, -0.18, 0.42, 0.18, -0.28);
        if (t < 292) return blend(t, 266, 292, 0.18, -0.28, 0, 0);
        return new Offset(0, 0);
    }

    private static Offset blend(float tick, float fromTick, float toTick,
                                double fromForward, double fromSide,
                                double toForward, double toSide) {
        float x = (tick - fromTick) / (toTick - fromTick);
        x = x * x * (3f - 2f * x);
        return new Offset(
            fromForward + (toForward - fromForward) * x,
            fromSide + (toSide - fromSide) * x
        );
    }

    public record Offset(double forward, double side) {}
}
