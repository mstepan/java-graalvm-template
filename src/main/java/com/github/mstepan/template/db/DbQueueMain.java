package com.github.mstepan.template.db;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.DriverManager;
import java.util.concurrent.StructuredTaskScope;

@SuppressFBWarnings(
        value = {"HARD_CODE_PASSWORD", "DMI_CONSTANT_DB_PASSWORD"},
        justification = "Test credentials for local Postgres")
public class DbQueueMain {

    private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/test_db";

    private static final String DB_USER = "test_user";

    private static final String DB_PASSWORD = "test_password";

    /*
    ==================================
    Writers
    ==================================
    Time elapsed: 3.2 seconds
    Total ops completed: 20000.0
    Throughput: 6281.93 ops/sec

    ==================================
    Readers
    ==================================
    Time elapsed: 9.0 seconds
    Total ops completed: 20000.0
    Throughput: 2215.44 ops/sec

    ==================================
    Mixed (readers + writers)
    ==================================
    Time elapsed: 12.2 seconds
    Total ops completed: 40000.0
    Throughput: 3291.42 ops/sec
     */
    @SuppressWarnings(value = {"preview", "CatchAndPrintStackTrace"})
    void main() throws Exception {

        // tasksCount should be limited to 20, otherwise we wil exhaust postgres connection pool
        final int tasksCount = 20;

        // iterations count can be increased to push more work on postgres
        final int iterationsCount = 1000;

        final long startTime = System.nanoTime();

        try (var scope = StructuredTaskScope.open()) {

            // fork writers
            for (int i = 0; i < tasksCount; ++i) {

                final int writerId = i;

                scope.fork(
                        () -> {
                            try (DbQueue queue =
                                    new DbQueue(
                                            DriverManager.getConnection(
                                                    JDBC_URL, DB_USER, DB_PASSWORD))) {
                                for (int it = 0; it < iterationsCount; ++it) {
                                    queue.add(String.format("writer-%d-message-%d", writerId, it));
                                }
                            } catch (Exception sqlEx) {
                                sqlEx.printStackTrace();
                            }
                        });
            }

            // fork readers
            for (int i = 0; i < tasksCount; ++i) {
                scope.fork(
                        () -> {
                            try (DbQueue queue =
                                    new DbQueue(
                                            DriverManager.getConnection(
                                                    JDBC_URL, DB_USER, DB_PASSWORD))) {

                                for (int it = 0; it < iterationsCount; ++it) {
                                    String _ = queue.poll();
                                }
                            } catch (Exception sqlEx) {
                                sqlEx.printStackTrace();
                            }
                        });
            }

            // throws exception HERE if any reader or writer fails
            scope.join();
        }

        final long endTime = System.nanoTime();

        double totalOpsCount = 2 * tasksCount * iterationsCount;
        double timeElapsedInSec = (endTime - startTime) / 1_000_000_000.0;
        double throughput = totalOpsCount / timeElapsedInSec;

        System.out.printf("Time elapsed: %.1f seconds%n", timeElapsedInSec);
        System.out.printf("Total ops completed: %.1f%n", totalOpsCount);
        System.out.printf("Throughput: %.2f ops/sec%n", throughput);

        System.out.println("DB queue test completed!!!");
    }
}
