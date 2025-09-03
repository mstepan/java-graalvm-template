package com.github.mstepan.template.probabalistic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

public class CountMinSketchTest {

    @Test
    void addAndCountFrequency() {
        CountMinSketch<String> sketch = new CountMinSketch<>();

        for (int i = 0; i < 10; ++i) {
            sketch.add("hello");
        }
        assertEqualsWithDeviation(10, sketch.countFrequency("hello"));

        for (int i = 0; i < 7; ++i) {
            sketch.add("world");
        }
        assertEqualsWithDeviation(7, sketch.countFrequency("world"));

        for (int i = 0; i < 13; ++i) {
            sketch.add("test-123");
        }
        assertEqualsWithDeviation(13, sketch.countFrequency("test-123"));
    }

    @SuppressWarnings("preview")
    @Test
    void addAndCountFrequencyMultipleThreads() throws Exception {
        final int threadsCount = 64;

        final List<StringAndCount> stringsAndCounts = new ArrayList<>();

        for (int wordIdx = 0; wordIdx < 1000; ++wordIdx) {
            StringAndCount strAndCount = randomStringAndCount();
            stringsAndCounts.add(strAndCount);
        }

        CountMinSketch<String> sketch = new CountMinSketch<>();

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            for (int threadsIdx = 0; threadsIdx < threadsCount; ++threadsIdx) {
                scope.fork(
                        () -> {
                            for (StringAndCount stringAndCount : stringsAndCounts) {
                                for (int i = 0; i < stringAndCount.count(); ++i) {
                                    sketch.add(stringAndCount.value());
                                }
                            }
                            return null;
                        });
            }

            scope.join();
            scope.throwIfFailed();
        }

        for (StringAndCount stringAndCount : stringsAndCounts) {
            long expectedFreq = ((long) stringAndCount.count()) * threadsCount;
            long actualFreq = sketch.countFrequency(stringAndCount.value());

            assertEqualsWithDeviation(expectedFreq, actualFreq);
        }
    }

    private void assertEqualsWithDeviation(long expectedFreq, long actualFreq) {
        final double expectedDeviation = CountMinSketch.DEFAULT_ESTIMATED_ERROR; // 0.001
        assertEquals((double) expectedFreq, (double) actualFreq, expectedDeviation);
    }

    record StringAndCount(String value, int count) {}

    private static StringAndCount randomStringAndCount() {
        int length = 10 + ThreadLocalRandom.current().nextInt(20);
        return new StringAndCount(randomString(length), randCount());
    }

    private static int randCount() {
        return 1 + ThreadLocalRandom.current().nextInt(1000);
    }

    private static String randomString(int length) {
        StringBuilder buf = new StringBuilder(length);

        ThreadLocalRandom rand = ThreadLocalRandom.current();

        for (int i = 0; i < length; i++) {
            char randCh = (char) ('a' + rand.nextInt('z' - 'a' + 1));
            buf.append(randCh);
        }

        return buf.toString();
    }
}
