package com.github.mstepan.template.db;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;

/**
 * Postgres as a Queue described here
 * https://topicpartition.io/blog/postgres-pubsub-queue-benchmarks
 */
public class DbQueue implements AutoCloseable {

    private static final String INSERT_INTO_QUEUE = "INSERT INTO queue (payload) VALUES (?)";
    private static final String SELECT_FROM_QUEUE_TABLE =
            "SELECT id, payload, created_at FROM queue ORDER BY id FOR UPDATE SKIP LOCKED LIMIT 1";
    private static final String DELETE_FROM_QUEUE = "DELETE FROM queue WHERE id = ?";

    private static final String INSERT_INTO_QUEUE_ARCHIVE =
            "INSERT INTO queue_archive(payload, created_at) VALUES (?, ?)";

    private final Connection connection;

    @SuppressFBWarnings(
            value = {"EI_EXPOSE_REP2"},
            justification =
                    "It's ok to store connection, otherwise we need to store jdbc parameters")
    public DbQueue(Connection connection) {
        this.connection = Objects.requireNonNull(connection);
    }

    void add(String message) {
        try (var ps = connection.prepareStatement(INSERT_INTO_QUEUE)) {
            ps.setBytes(1, message.getBytes(StandardCharsets.UTF_8));
            var insertedRowsCount = ps.executeUpdate();
            if (insertedRowsCount != 1) {
                throw new IllegalStateException(
                        String.format(
                                "Insert into queue failed, expected inserted row count 1, actual found %d",
                                insertedRowsCount));
            }
        } catch (SQLException sqlEx) {
            throw new IllegalStateException(sqlEx);
        }
    }

    String poll() {
        try {
            var prevAutocommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                var queueRow = readNotLockedRowFromQueueTable();

                if (queueRow == null) {
                    return null;
                }
                String retMessage = new String(queueRow.payload(), StandardCharsets.UTF_8);

                insertRowIntoArchiveTable(queueRow.payload(), queueRow.createdAt());

                deleteRowFromQueueTable(queueRow.id());

                connection.commit();

                return retMessage;
            } catch (SQLException sqlEx) {
                connection.rollback();
                throw new IllegalStateException(sqlEx);
            } finally {
                connection.setAutoCommit(prevAutocommit);
            }
        } catch (SQLException sqlEx) {
            throw new IllegalStateException(sqlEx);
        }
    }

    private QueueRow readNotLockedRowFromQueueTable() throws SQLException {
        try (var selectFromQueueStatement = connection.prepareStatement(SELECT_FROM_QUEUE_TABLE)) {

            var rs = selectFromQueueStatement.executeQuery();

            if (rs.next()) {
                var id = rs.getLong(1);
                var payload = rs.getBytes(2);
                var createdAt = rs.getDate(3);

                return new QueueRow(id, payload, createdAt);
            }

            return null;
        }
    }

    @SuppressWarnings("JavaUtilDate")
    private void insertRowIntoArchiveTable(byte[] payload, Date createdAt) throws SQLException {
        try (var insertStatement = connection.prepareStatement(INSERT_INTO_QUEUE_ARCHIVE)) {

            insertStatement.setBytes(1, payload);
            insertStatement.setDate(2, new java.sql.Date(createdAt.getTime()));

            int insertedRowsCount = insertStatement.executeUpdate();

            if (insertedRowsCount != 1) {
                throw new IllegalStateException(
                        String.format(
                                "Insert into queue archive failed, expected inserted row count 1, actual found %d",
                                insertedRowsCount));
            }
        }
    }

    private void deleteRowFromQueueTable(long id) throws SQLException {
        try (var deleteStatement = connection.prepareStatement(DELETE_FROM_QUEUE)) {

            deleteStatement.setLong(1, id);

            int deletedRowsCount = deleteStatement.executeUpdate();

            if (deletedRowsCount != 1) {
                throw new IllegalStateException(
                        String.format(
                                "Delete row from queue failed, expected deleted row count 1, actual found %d",
                                deletedRowsCount));
            }
        }
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }

    @SuppressWarnings("ArrayRecordComponent")
    record QueueRow(long id, byte[] payload, Date createdAt) {}
}
