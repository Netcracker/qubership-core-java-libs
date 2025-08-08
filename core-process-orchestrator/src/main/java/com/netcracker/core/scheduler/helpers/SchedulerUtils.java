package com.netcracker.core.scheduler.helpers;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;

public class SchedulerUtils {
    public static Scheduler scheduler;


    public static DataSource initDatabase() {
        return initDatabase("jdbc:h2:mem:schedule_testing");
    }

    public static DataSource initDatabase(String db) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(db);
        config.setUsername("sa");
        config.setPassword("");
        config.setAutoCommit(true);
        HikariDataSource dataSource = new HikariDataSource(config);

        doWithConnection(dataSource, c -> {
            try (Statement statement = c.createStatement()) {
                statement.execute(readDBDefinition());
            } catch (Exception e) {
                throw new RuntimeException("Failed to create tables", e);
            }
        });

        return dataSource;
    }

    private static String readDBDefinition() throws IOException {

        try (InputStream stream = SchedulerUtils.class.getResourceAsStream("/hsql_tables.sql")) {
            if (stream != null) {
                return new String(stream.readAllBytes());
            }
        }
        return null;
    }

    private static void doWithConnection(DataSource dataSource, Consumer<Connection> consumer) {
        try (Connection connection = dataSource.getConnection()) {
            consumer.accept(connection);
        } catch (SQLException e) {
            throw new RuntimeException("Error getting connection from datasource.", e);
        }
    }


}
