package dev.sardoryusupov.pairedanimations;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TangoTimelineTest {
    @Test void returnsNeutralPoseAtStartAndFinish() {
        var start = TangoTimeline.poseAt(0, true);
        var end = TangoTimeline.poseAt(TangoTimeline.DURATION_TICKS, true);
        assertEquals(0f, start.leftArmX(), 0.00001f);
        assertEquals(0f, start.bodyY(), 0.00001f);
        assertEquals(0f, end.leftArmX(), 0.00001f);
        assertEquals(0f, end.rightLegX(), 0.00001f);
    }

    @Test void clampsOutsideDuration() {
        assertEquals(TangoTimeline.poseAt(0, true),
                     TangoTimeline.poseAt(-100, true));
        assertEquals(TangoTimeline.poseAt(320, false),
                     TangoTimeline.poseAt(100000, false));
    }

    @Test void partnersReceiveMirroredDirections() {
        var a = TangoTimeline.poseAt(140, true);
        var b = TangoTimeline.poseAt(140, false);
        assertEquals(a.leftLegX(), -b.leftLegX(), 0.0001f);
        assertEquals(a.leftArmY(), -b.leftArmY(), 0.0001f);
    }

    @Test void posesRemainFinite() {
        for (int i = 0; i <= TangoTimeline.DURATION_TICKS; i++) {
            var p = TangoTimeline.poseAt(i, i % 2 == 0);
            assertTrue(Float.isFinite(p.bodyY()));
            assertTrue(Float.isFinite(p.leftLegX()));
            assertTrue(Float.isFinite(p.rightArmX()));
        }
    }
}
