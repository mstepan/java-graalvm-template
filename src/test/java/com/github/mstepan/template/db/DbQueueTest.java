package com.github.mstepan.template.db;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class DbQueueTest {
    @Test
    void addBindsUtf8AndClosesStatement() {
        Jdbc jdbc = new Jdbc();
        new DbQueue(jdbc.connection()).add("héllo");
        assertArrayEquals(
                "héllo".getBytes(StandardCharsets.UTF_8), (byte[]) jdbc.parameters.get(1));
        assertEquals(List.of("statement.close"), jdbc.events);
    }

    @Test
    void addRejectsUnexpectedRowCount() {
        Jdbc jdbc = new Jdbc();
        jdbc.insertCount = 0;
        assertThrows(IllegalStateException.class, () -> new DbQueue(jdbc.connection()).add("x"));
        assertTrue(jdbc.events.contains("statement.close"));
    }

    @Test
    void addWrapsSqlFailure() {
        Jdbc jdbc = new Jdbc();
        jdbc.failure = "prepareStatement";
        IllegalStateException error =
                assertThrows(
                        IllegalStateException.class, () -> new DbQueue(jdbc.connection()).add("x"));
        assertInstanceOf(SQLException.class, error.getCause());
    }

    @Test
    void pollArchivesDeletesAndCommitsBeforeRestoringAutoCommit() {
        Jdbc jdbc = new Jdbc();
        assertEquals("héllo", new DbQueue(jdbc.connection()).poll());
        assertEquals(7L, jdbc.parameters.get(1));
        assertEquals(Date.valueOf("2025-01-02"), jdbc.parameters.get(2));
        assertEquals(
                List.of(
                        "autoCommit:false",
                        "statement.close",
                        "statement.close",
                        "statement.close",
                        "commit",
                        "autoCommit:true"),
                jdbc.events);
    }

    @Test
    void emptyPollRestoresOriginalAutoCommit() {
        Jdbc jdbc = new Jdbc();
        jdbc.hasRow = false;
        jdbc.autoCommit = false;
        assertNull(new DbQueue(jdbc.connection()).poll());
        assertEquals(
                List.of("autoCommit:false", "statement.close", "autoCommit:false"), jdbc.events);
    }

    @Test
    void pollRollsBackSqlFailureAndRestoresAutoCommit() {
        Jdbc jdbc = new Jdbc();
        jdbc.failure = "executeQuery";
        IllegalStateException error =
                assertThrows(
                        IllegalStateException.class, () -> new DbQueue(jdbc.connection()).poll());
        assertInstanceOf(SQLException.class, error.getCause());
        assertEquals(
                List.of("autoCommit:false", "statement.close", "rollback", "autoCommit:true"),
                jdbc.events);
    }

    @Test
    void pollRollsBackUnexpectedArchiveAndDeleteCounts() {
        for (boolean archiveFailure : List.of(true, false)) {
            Jdbc jdbc = new Jdbc();
            jdbc.archiveCount = archiveFailure ? 0 : 1;
            jdbc.deleteCount = archiveFailure ? 1 : 0;
            assertThrows(IllegalStateException.class, () -> new DbQueue(jdbc.connection()).poll());
            assertTrue(jdbc.events.contains("rollback"));
            assertFalse(jdbc.events.contains("commit"));
            assertEquals("autoCommit:true", jdbc.events.getLast());
        }
    }

    @Test
    void pollWrapsFailureToReadAutoCommit() {
        Jdbc jdbc = new Jdbc();
        jdbc.failure = "getAutoCommit";
        IllegalStateException error =
                assertThrows(
                        IllegalStateException.class, () -> new DbQueue(jdbc.connection()).poll());
        assertInstanceOf(SQLException.class, error.getCause());
    }

    @Test
    void closeClosesConnectionAndPropagatesSqlFailure() throws SQLException {
        Jdbc jdbc = new Jdbc();
        DbQueue queue = new DbQueue(jdbc.connection());
        queue.close();
        assertEquals(List.of("connection.close"), jdbc.events);
        jdbc.failure = "close";
        assertThrows(SQLException.class, queue::close);
    }

    static final class Jdbc {
        final List<String> events = new ArrayList<>();
        final Map<Integer, Object> parameters = new HashMap<>();
        String failure = "";
        boolean hasRow = true;
        boolean autoCommit = true;
        int adds;
        int polls;
        int insertCount = 1;
        int archiveCount = 1;
        int deleteCount = 1;

        Connection connection() {
            return proxy(
                    Connection.class,
                    (_, method, args) -> {
                        failIfRequested(method.getName());
                        return switch (method.getName()) {
                            case "prepareStatement" -> statement((String) requireArgs(args)[0]);
                            case "getAutoCommit" -> autoCommit;
                            case "setAutoCommit" -> {
                                autoCommit = (boolean) requireArgs(args)[0];
                                events.add("autoCommit:" + autoCommit);
                                yield null;
                            }
                            case "commit", "rollback" -> {
                                events.add(method.getName());
                                yield null;
                            }
                            case "close" -> {
                                events.add("connection.close");
                                yield null;
                            }
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    });
        }

        private PreparedStatement statement(String sql) {
            return proxy(
                    PreparedStatement.class,
                    (_, method, args) -> {
                        failIfRequested(method.getName());
                        return switch (method.getName()) {
                            case "setBytes", "setDate", "setLong" -> {
                                Object[] values = requireArgs(args);
                                parameters.put((Integer) values[0], values[1]);
                                yield null;
                            }
                            case "executeUpdate" -> {
                                if (sql.startsWith("INSERT INTO queue (")) {
                                    ++adds;
                                }
                                yield sql.startsWith("DELETE")
                                        ? deleteCount
                                        : sql.startsWith("INSERT INTO queue_archive")
                                                ? archiveCount
                                                : insertCount;
                            }
                            case "executeQuery" -> {
                                ++polls;
                                yield resultSet();
                            }
                            case "close" -> {
                                events.add("statement.close");
                                yield null;
                            }
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    });
        }

        private ResultSet resultSet() {
            return proxy(
                    ResultSet.class,
                    (_, method, _) ->
                            switch (method.getName()) {
                                case "next" -> hasRow;
                                case "getLong" -> 7L;
                                case "getBytes" -> "héllo".getBytes(StandardCharsets.UTF_8);
                                case "getDate" -> Date.valueOf("2025-01-02");
                                default ->
                                        throw new UnsupportedOperationException(method.getName());
                            });
        }

        private void failIfRequested(String method) throws SQLException {
            if (failure.equals(method)) {
                throw new SQLException("Expected test failure");
            }
        }

        private static Object[] requireArgs(Object @Nullable [] args) {
            return java.util.Objects.requireNonNull(args);
        }

        private static <T> T proxy(Class<T> type, InvocationHandler handler) {
            return type.cast(
                    Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
        }
    }
}
