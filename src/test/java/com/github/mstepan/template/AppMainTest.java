package com.github.mstepan.template;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class AppMainTest {

    @Test
    @Timeout(15)
    void reportsTaskFailureAndFinishes() {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        var output = new ByteArrayOutputStream();
        var errors = new ByteArrayOutputStream();
        try (var out = new PrintStream(output, true, StandardCharsets.UTF_8);
                var err = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            System.setOut(out);
            System.setErr(err);
            AppMain.main();
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Main done..."));
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("Division by 0"));
        assertFalse(output.toString(StandardCharsets.UTF_8).contains("All tasks completed"));
    }

    @Test
    @Timeout(15)
    void preservesCallerInterruption() {
        Thread.currentThread().interrupt();
        try {
            AppMain.main();
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
