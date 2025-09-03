package com.github.mstepan.template.probabalistic;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicLongArray;

/** Thread-safe count-min sketch implementation. */
public final class CountMinSketch<T> {

    static final double DEFAULT_ESTIMATED_ERROR = 0.001; // 0.1%
    private static final double DEFAULT_CONFIDENCE = 0.99999; // 99.999%

    private final int rows;
    private final int cols;

    private final AtomicLongArray[] counters;
    private final Hash<T>[] hashes;

    @SuppressWarnings("unchecked")
    public CountMinSketch() {
        rows = calculateHeight(DEFAULT_CONFIDENCE);
        cols = calculateWidth(DEFAULT_ESTIMATED_ERROR);

        counters = new AtomicLongArray[rows];
        hashes = (Hash<T>[]) Array.newInstance(Hash.class, rows);

        for (int i = 0; i < rows; i++) {
            counters[i] = new AtomicLongArray(cols);
            hashes[i] = new Hash<>();
        }

        System.out.printf("CountMinSketch size (rows x cols): %d x %d%n", rows, cols);
    }

    public void add(T value) {
        for (int i = 0; i < rows; i++) {
            counters[i].incrementAndGet(hashIdx(value, i));
        }
    }

    public long countFrequency(T value) {
        long minValue = Long.MAX_VALUE;

        for (int i = 0; i < rows; i++) {
            minValue = Math.min(minValue, counters[i].get(hashIdx(value, i)));
        }

        return (int) minValue;
    }

    private int hashIdx(T value, int row) {
        int idx = (int) (hashes[row].hash(value) % cols);
        return idx >= 0 ? idx : -idx;
    }

    private int calculateWidth(double estimatedError) {
        return (int) (Math.E / estimatedError) + 1;
    }

    private int calculateHeight(double confidence) {
        return (int) Math.log(1.0 / (1.0 - confidence)) + 1;
    }
}
