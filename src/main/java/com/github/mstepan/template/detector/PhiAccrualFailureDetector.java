package com.github.mstepan.template.detector;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Phi accrual failure detector utilities.
 *
 * <p>This implementation models heartbeat delays as normally distributed and computes phi =
 * -log10(P(X > currentDelay)), where X ~ Normal(mean, stddev) learned from a bounded sliding window
 * of recent delays.
 *
 * <p>Notes: - Returns 0.0 until at least {@code minSamples} observations are collected (warm-up). -
 * Uses sample standard deviation (n-1) and guards with {@code minStdDev} to avoid division by zero.
 * - Applies a probability floor to avoid infinite phi due to underflow for extreme z-scores. -
 * Thread-safe for concurrent addDelay/computePhi via synchronization. - Units: delay values should
 * be provided in consistent time units (e.g., milliseconds).
 */
public final class PhiAccrualFailureDetector {

    private static final int DEFAULT_WINDOW_SIZE = 100;
    private static final int DEFAULT_MIN_SAMPLES = 10;
    private static final double DEFAULT_MIN_STDDEV = 1e-3; // in delay units
    private static final double DEFAULT_PROBABILITY_FLOOR = 1e-12;

    private final int windowSize;
    private final int minSamples;
    private final double minStdDev;
    private final double probabilityFloor;

    private final Deque<Long> heartbeatDelays;

    /** Creates a detector with sensible defaults. */
    public PhiAccrualFailureDetector() {
        this(
                DEFAULT_WINDOW_SIZE,
                DEFAULT_MIN_SAMPLES,
                DEFAULT_MIN_STDDEV,
                DEFAULT_PROBABILITY_FLOOR);
    }

    private PhiAccrualFailureDetector(
            int windowSize, int minSamples, double minStdDev, double probabilityFloor) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be > 0");
        }
        if (minSamples <= 0) {
            throw new IllegalArgumentException("minSamples must be > 0");
        }
        if (minStdDev <= 0.0) {
            throw new IllegalArgumentException("minStdDev must be > 0");
        }
        if (!(probabilityFloor > 0.0 && probabilityFloor < 1.0)) {
            throw new IllegalArgumentException("probabilityFloor must be in (0,1)");
        }
        if (minSamples > windowSize) {
            throw new IllegalArgumentException("minSamples must be <= windowSize");
        }

        this.windowSize = windowSize;
        this.minSamples = minSamples;
        this.minStdDev = minStdDev;
        this.probabilityFloor = probabilityFloor;
        this.heartbeatDelays = new ArrayDeque<>(windowSize);
    }

    /**
     * Adds a new observed heartbeat delay to the sliding window.
     *
     * @param delay observed delay (same time units used consistently across calls)
     */
    public void addDelay(Duration delay) {
        long delayInMs = delay.toMillis();

        if (delayInMs <= 0L) {
            throw new IllegalArgumentException("'delay' must >= 0");
        }
        synchronized (heartbeatDelays) {
            if (heartbeatDelays.size() == windowSize) {
                heartbeatDelays.removeFirst();
            }
            heartbeatDelays.addLast(delayInMs);
        }
    }

    /**
     * Computes phi for the provided current delay using the learned distribution from the sliding
     * window.
     *
     * <p>phi threshold often used in systems like Cassandra/Akka is around 8.0.
     *
     * @param curDelay current observed delay
     * @return phi value; returns 0.0 if not enough samples are available
     */
    public double computePhi(Duration curDelay) {
        final long[] sample;
        synchronized (heartbeatDelays) {
            if (heartbeatDelays.size() < minSamples) {
                return 0.0; // warm-up: insufficient data
            }
            sample = new long[heartbeatDelays.size()];
            int i = 0;
            for (Long delay : heartbeatDelays) {
                sample[i++] = delay;
            }
        }

        final double mean = mean(sample);
        double std = standardDeviationFromSamples(sample, mean);
        if (std < minStdDev) {
            std = minStdDev;
        }

        final double curDelayInMs = curDelay.toMillis();
        if (curDelayInMs <= 0L) {
            throw new IllegalArgumentException("'curDelay' must be >= 0");
        }

        final double zScore = (curDelayInMs - mean) / std;
        double pGreater = 1.0 - standardNormalCDF(zScore);
        if (pGreater < probabilityFloor) {
            pGreater = probabilityFloor;
        } else if (pGreater > 1.0) { // numerical safety
            pGreater = 1.0;
        }
        return -Math.log10(pGreater);
    }

    private static double mean(long[] values) {
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    // Sample standard deviation (n-1)
    private static double standardDeviationFromSamples(long[] values, double mean) {
        if (values.length <= 1) {
            return 0.0;
        }
        double sumSq = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / (values.length - 1));
    }

    // Standard normal CDF using erf approximation
    private static double standardNormalCDF(double z) {
        return 0.5 * (1.0 + erf(z / Math.sqrt(2.0)));
    }

    // Copied from https://introcs.cs.princeton.edu/java/21function/ErrorFunction.java.html
    // fractional error less than x.xx * 10 ^ -4.
    // Algorithm 26.2.17 in Abromowitz and Stegun, Handbook of Mathematical.
    private static double erf(double z) {
        final double t = 1.0 / (1.0 + 0.47047 * Math.abs(z));
        final double poly = t * (0.3480242 + t * (-0.0958798 + t * (0.7478556)));
        final double ans = 1.0 - poly * Math.exp(-z * z);
        return z >= 0 ? ans : -ans;
    }
}
