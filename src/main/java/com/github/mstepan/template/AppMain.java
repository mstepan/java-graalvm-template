package com.github.mstepan.template;

import com.github.mstepan.template.concurrent.AllSuccessfulOrFailWithRateLimiterJoiner;
import java.time.Duration;
import java.util.concurrent.StructuredTaskScope;

@SuppressWarnings("preview")
public class AppMain {

    static void main() {

        try (var scope =
                StructuredTaskScope.open(new AllSuccessfulOrFailWithRateLimiterJoiner<>(3))) {

            for (int i = 0; i < 10; ++i) {

                final int id = i;

                var _ =
                        scope.fork(
                                () -> {
                                    if (id == 5) {
                                        System.out.println("Task will fail");
                                        throw new ArithmeticException("Division by 0");
                                    }

                                    System.out.printf(
                                            "[%s] started%n", Thread.currentThread().threadId());
                                    try {
                                        Thread.sleep(Duration.ofSeconds(3L));
                                    } catch (InterruptedException interEx) {
                                        Thread.currentThread().interrupt();
                                        System.out.println("Cancelled");
                                    }

                                    System.out.printf(
                                            "[%s] completed%n", Thread.currentThread().threadId());

                                    return "result-" + id;
                                });
            }

            var _ = scope.join();

            System.out.println("All tasks completed");

        } catch (StructuredTaskScope.FailedException failedEx) {
            if (failedEx.getCause() != null) {
                System.err.println(failedEx.getCause().getMessage());
            } else {
                System.err.println("Failed unknow cause");
            }
        } catch (InterruptedException interEx) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Main done...");
    }
}
