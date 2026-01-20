package com.github.mstepan.template.detector;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;

public class PhiAccrualFailureDetectorTest {

    @Test
    void checkPhiValue() {
        PhiAccrualFailureDetector phiAccrualFailureDetector = new PhiAccrualFailureDetector();

        long[] delays = {1030, 1100, 1200, 1300, 1500, 1700, 2000, 1400, 2000, 1850};

        for (long delay : delays) {
            phiAccrualFailureDetector.addDelay(Duration.ofMillis(delay));
        }

        assertEquals(1.1, phiAccrualFailureDetector.computePhi(Duration.ofMillis(2000)), 0.1);
        assertEquals(4.7, phiAccrualFailureDetector.computePhi(Duration.ofMillis(3000)), 0.1);
        assertEquals(12.0, phiAccrualFailureDetector.computePhi(Duration.ofMillis(5000)), 0.1);
    }

    @Test
    void warmUpReturnsZeroUntilMinSamples() {
        PhiAccrualFailureDetector detector = new PhiAccrualFailureDetector();

        // fewer than default minSamples (10)
        for (int i = 0; i < 9; i++) {
            detector.addDelay(Duration.ofMillis(1000));
        }
        assertEquals(0.0, detector.computePhi(Duration.ofMillis(1000)), 1e-9);

        // reaching minSamples should produce non-zero phi
        detector.addDelay(Duration.ofMillis(1000)); // 10th sample
        double phiAtMean = detector.computePhi(Duration.ofMillis(1000));
        // with z=0 -> P(X>mean)=0.5 -> phi ~= 0.3010
        assertEquals(0.301, phiAtMean, 0.05);
    }

    @Test
    void rejectsNonPositiveDelays() {
        PhiAccrualFailureDetector detector = new PhiAccrualFailureDetector();

        assertThrows(IllegalArgumentException.class, () -> detector.addDelay(Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class, () -> detector.addDelay(Duration.ofMillis(-5)));

        // warm up to avoid warm-up early return
        for (int i = 0; i < 10; i++) {
            detector.addDelay(Duration.ofMillis(1000));
        }
        assertThrows(IllegalArgumentException.class, () -> detector.computePhi(Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class, () -> detector.computePhi(Duration.ofMillis(-1)));
    }

    @Test
    void slidingWindowEvictsOldestValues() {
        PhiAccrualFailureDetector detector = new PhiAccrualFailureDetector();

        // fill window with 1000ms values (default window size = 100)
        for (int i = 0; i < 100; i++) {
            detector.addDelay(Duration.ofMillis(1000));
        }
        // add 50 values of 2000ms -> last 100 contain 50 of 1000 and 50 of 2000, mean=1500
        for (int i = 0; i < 50; i++) {
            detector.addDelay(Duration.ofMillis(2000));
        }
        double phiAtMean = detector.computePhi(Duration.ofMillis(1500));
        assertEquals(0.301, phiAtMean, 0.05);
    }

    @Test
    void phiIncreasesWithLargerCurrentDelay() {
        PhiAccrualFailureDetector detector = new PhiAccrualFailureDetector();
        long[] delays = {1000, 1100, 1200, 1300, 1500, 1700, 2000, 1400, 2000, 1850};
        for (long d : delays) {
            detector.addDelay(Duration.ofMillis(d));
        }
        double phi1500 = detector.computePhi(Duration.ofMillis(1500));
        double phi2000 = detector.computePhi(Duration.ofMillis(2000));
        double phi3000 = detector.computePhi(Duration.ofMillis(3000));

        assertTrue(phi1500 < phi2000);
        assertTrue(phi2000 < phi3000);
    }
}
