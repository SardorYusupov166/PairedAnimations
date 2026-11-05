package dev.sardoryusupov.pairedanimations;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TangoFootworkTest {
    @Test void returnsToStartingPoint() {
        assertEquals(new TangoFootwork.Offset(0, 0), TangoFootwork.at(0));
        assertEquals(new TangoFootwork.Offset(0, 0), TangoFootwork.at(320));
        assertEquals(TangoFootwork.at(0), TangoFootwork.at(-1));
        assertEquals(TangoFootwork.at(320), TangoFootwork.at(500));
    }

    @Test void respectsConservativeStepSizes() {
        for (float t = 0; t <= 320; t += 0.25f) {
            var step = TangoFootwork.at(t);
            assertTrue(Double.isFinite(step.forward()));
            assertTrue(Double.isFinite(step.side()));
            assertTrue(Math.abs(step.forward()) <= 0.43);
            assertTrue(Math.abs(step.side()) <= 0.43);
            var next = TangoFootwork.at(Math.min(320, t + 1));
            assertTrue(Math.hypot(next.forward() - step.forward(),
                                  next.side() - step.side()) <= 0.045);
        }
    }

    @Test void followsDramaticSteps() {
        assertTrue(TangoFootwork.at(70).forward() > 0.3);
        assertTrue(TangoFootwork.at(132).forward() < 0);
        assertTrue(TangoFootwork.at(200).side() > 0.3);
        assertTrue(TangoFootwork.at(266).side() < 0);
    }
}
