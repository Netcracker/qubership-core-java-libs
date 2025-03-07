package org.qubership.cloud.dbaas.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ContextConfiguration(classes = {TestFlywayConfig.class, PostgresTestContainerConfiguration.class})
@TestPropertySource(properties = {
        "dbaas.postgres.datasource.flyway.baselineOnMigrate=true",
        "dbaas.postgres.datasource.flyway.locations=classpath:db/migration/default",
        "dbaas.postgres.datasources.configs.flyway.baselineOnMigrate=true",
        "dbaas.postgres.datasources.configs.flyway.ignoreMigrationPatterns=*:future, *:missing",
        "dbaas.postgres.datasources.configs.flyway.locations=classpath:db/migration/versioned"
})
public class FlywayPostgresPostProcessorTest {

    @Autowired
    private DataSource postgresDataSource;

    @Autowired
    @Qualifier("serviceVersionedDatasource")
    private DataSource versionedPgDatasource;

    @Test
    void testMigrationsAreExecutedForCorrectDatasource() throws SQLException {
        String defaultTableName = "message_default";
        String versionedTableName = "message_versioned";
        checkShouldTablePresentInDatabase(postgresDataSource, defaultTableName, true);
        checkShouldTablePresentInDatabase(postgresDataSource, versionedTableName, false);
        checkShouldTablePresentInDatabase(versionedPgDatasource, defaultTableName, false);
        checkShouldTablePresentInDatabase(versionedPgDatasource, versionedTableName, true);
    }

    private void checkShouldTablePresentInDatabase(DataSource postgresDataSource, String tableName, boolean shouldPresent) throws SQLException {
        try (Connection connection = postgresDataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM pg_catalog.pg_tables where tablename= ?;")) {
            preparedStatement.setString(1, tableName);
            ResultSet resultSet = preparedStatement.executeQuery();
            boolean isPresent = resultSet.next();
            if (shouldPresent) {
                assertTrue(isPresent);
            } else {
                assertFalse(isPresent);
            }
        }
    }
}
