package com.github.mstepan.template;

import com.github.mstepan.template.probabalistic.CountMinSketch;
import java.util.concurrent.StructuredTaskScope;

public class AppMain {

    @SuppressWarnings("preview")
    public static void main(String[] args) throws Exception {

        final CountMinSketch<String> sketch = new CountMinSketch<>();

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            for (int threadsIdx = 0; threadsIdx < 100; ++threadsIdx) {
                scope.fork(
                        () -> {
                            for (int it = 0; it < 1000; ++it) {
                                sketch.add("hello");
                            }

                            for (int it = 0; it < 1000; ++it) {
                                sketch.add("world");
                            }

                            return null;
                        });
            }

            scope.join();
            scope.throwIfFailed();

            long helloFreq = sketch.estimateFrequency("hello");
            System.out.printf("'hello' frequency: %d%n", helloFreq);

            long worldFreq = sketch.estimateFrequency("world");
            System.out.printf("'world' frequency: %d%n", worldFreq);
        }

        System.out.printf("Java version: %s. Main done...%n", System.getProperty("java.version"));
    }
}
