package com.github.mstepan.template.failire_detector;

import java.util.ArrayList;
import java.util.List;

public final class PhiAccrualFailureDetector {

    private final List<Double> heartbeatDelays = new ArrayList<>();

    // Compute phi for a heartbeat
    // phi threshold for Cassandra and Akka = 8
    public double computePhi(double curDelay) {
        double mean = mean();
        double standardDeviation = standardDeviation(mean);
        double zScore = (curDelay - mean) / standardDeviation;
        double pGreater = 1 - standardNormalCDF(zScore);
        return -Math.log10(pGreater);
    }

    private double mean() {
        double result = 0.0;

        for (double delay : heartbeatDelays) {
            result += delay;
        }

        return result / heartbeatDelays.size();
    }

    private double standardDeviation(double mean) {
        double result = 0.0;

        for (double delay : heartbeatDelays) {
            double diff = delay - mean;
            result += (diff * diff);
        }

        return Math.sqrt(result / heartbeatDelays.size());
    }


    // Standard normal CDF using erf
    private static double standardNormalCDF(double z) {
        return 0.5 * (1 + erf(z / Math.sqrt(2)));
    }

    // Copied from https://introcs.cs.princeton.edu/java/21function/ErrorFunction.java.html
    // fractional error less than x.xx * 10 ^ -4.
    // Algorithm 26.2.17 in Abromowitz and Stegun, Handbook of Mathematical.
    public static double erf(double z) {
        double t = 1.0 / (1.0 + 0.47047 * Math.abs(z));
        double poly = t * (0.3480242 + t * (-0.0958798 + t * (0.7478556)));
        double ans = 1.0 - poly * Math.exp(-z * z);
        if (z >= 0) {
            return ans;
        } else {
            return -ans;
        }
    }

    public void addDelay(double delay) {
        heartbeatDelays.add(delay);
    }
}
