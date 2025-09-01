package com.github.mstepan.template.probabalistic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.StructuredTaskScope;
import org.junit.jupiter.api.Test;

public class CountMinSketchTest {

    @Test
    void addAndCountFrequency() {
        CountMinSketch<String> sketch = new CountMinSketch<>();

        for (int i = 0; i < 10; ++i) {
            sketch.add("hello");
        }
        assertEquals(10, sketch.countFrequency("hello"));

        for (int i = 0; i < 7; ++i) {
            sketch.add("world");
        }
        assertEquals(7, sketch.countFrequency("world"));

        for (int i = 0; i < 13; ++i) {
            sketch.add("test-123");
        }
        assertEquals(13, sketch.countFrequency("test-123"));
    }

    @SuppressWarnings("preview")
    @Test
    void addAndCountFrequencyMultipleThreads() throws Exception {
        final int threadsCount = 10;

        final String word1 = "hello";
        final int itCount1 = 1333;

        final String word2 = "world";
        final int itCount2 = 1777;

        CountMinSketch<String> sketch = new CountMinSketch<>();

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            for (int threadsIdx = 0; threadsIdx < threadsCount; ++threadsIdx) {
                scope.fork(
                        () -> {
                            for (int it = 0; it < itCount1; ++it) {
                                sketch.add(word1);
                            }

                            for (int it = 0; it < itCount2; ++it) {
                                sketch.add(word2);
                            }

                            return null;
                        });
            }

            scope.join();
            scope.throwIfFailed();
        }

        assertEquals(threadsCount * itCount1, sketch.countFrequency(word1));
        assertEquals(threadsCount * itCount2, sketch.countFrequency(word2));
    }
}
