package com.github.mstepan.template.db;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.StructuredTaskScope;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class DbQueueMainTest {
    @Test
    void connectionFailurePropagatesFromTheScope() {
        var failure =
                assertThrows(
                        StructuredTaskScope.FailedException.class,
                        () -> new DbQueueMain().run("jdbc:dbqueue-test:unregistered"));
        assertInstanceOf(SQLException.class, failure.getCause());
    }

    @Test
    void mainRunsAllReadersAndWritersAndClosesConnections()
            throws SQLException, InterruptedException {
        List<DbQueueTest.Jdbc> connections = new CopyOnWriteArrayList<>();
        Driver driver =
                new Driver() {
                    @Override
                    public @Nullable Connection connect(String url, Properties info) {
                        if (!acceptsURL(url)) {
                            return null;
                        }
                        assertEquals("test_user", info.getProperty("user"));
                        DbQueueTest.Jdbc jdbc = new DbQueueTest.Jdbc();
                        connections.add(jdbc);
                        return jdbc.connection();
                    }

                    @Override
                    public boolean acceptsURL(String url) {
                        return url.equals("jdbc:dbqueue-test:benchmark");
                    }

                    @Override
                    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
                        return new DriverPropertyInfo[0];
                    }

                    @Override
                    public int getMajorVersion() {
                        return 1;
                    }

                    @Override
                    public int getMinorVersion() {
                        return 0;
                    }

                    @Override
                    public boolean jdbcCompliant() {
                        return false;
                    }

                    @Override
                    public Logger getParentLogger() {
                        return Logger.getLogger("DbQueueMainTest");
                    }
                };
        DriverManager.registerDriver(driver);
        try {
            new DbQueueMain().run("jdbc:dbqueue-test:benchmark");
        } finally {
            DriverManager.deregisterDriver(driver);
        }
        assertEquals(40, connections.size());
        assertEquals(20_000, connections.stream().mapToInt(jdbc -> jdbc.adds).sum());
        assertEquals(20_000, connections.stream().mapToInt(jdbc -> jdbc.polls).sum());
        for (DbQueueTest.Jdbc jdbc : connections) {
            assertEquals("connection.close", jdbc.events.getLast());
        }
    }
}
