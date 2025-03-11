package org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers;

import org.qubership.cloud.core.quarkus.dbaas.datasource.DbaasQuarkusPostgresqlDatasourceBuilder;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.DbaaSPostgresDbCreationService;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.PostgresqlContainerResource;
import org.qubership.cloud.dbaas.client.service.flyway.FlywayRunner;
import org.qubership.cloud.dbaas.common.classifier.ServiceClassifierBuilder;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import org.flywaydb.core.Flyway;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

@QuarkusTestResource(PostgresqlContainerResource.class)
@QuarkusTest
@Slf4j
public class DbaasQuarkusDatasourceBuilderFlywayTest {

    @Inject
    DbaaSPostgresDbCreationService dbaaSPostgresDbCreationService;

    @Test
    void test() throws SQLException {
        DbaasQuarkusPostgresqlDatasourceBuilder builder = new DbaasQuarkusPostgresqlDatasourceBuilder(dbaaSPostgresDbCreationService);
        DataSource dataSource = builder.newBuilder(new ServiceClassifierBuilder(new HashMap<>()))
                .withFlyway(getFlywayRunner()).build();
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();

        String selectSql = "SELECT name FROM city";
        ResultSet resultSet = statement.executeQuery(selectSql);
        List<String> cities = new ArrayList<>();
        // Print results from select statement
        while (resultSet.next()) {
            cities.add(resultSet.getString(1));
        }
        Assertions.assertEquals(1, cities.size());
        Assertions.assertEquals("Moscow", cities.get(0));
    }

    @NotNull
    private static FlywayRunner getFlywayRunner() {
        return context -> {
            Flyway flyway = Flyway.configure()
                    .dataSource(context.getDataSource())
                    .baselineOnMigrate(true)
                    .locations("classpath:db/configs")
                    .load();
            flyway.migrate();
        };
    }
}
