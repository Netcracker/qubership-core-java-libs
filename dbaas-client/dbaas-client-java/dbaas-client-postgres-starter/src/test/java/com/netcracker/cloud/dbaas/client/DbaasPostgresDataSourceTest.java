package com.netcracker.cloud.dbaas.client;

import com.netcracker.cloud.dbaas.client.testconfiguration.PostgresTestContainerConfiguration;
import com.netcracker.cloud.dbaas.client.testconfiguration.TestPostgresWithDatasourceConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
@SpringBootTest
@ContextConfiguration(classes = {TestPostgresWithDatasourceConfig.class, PostgresTestContainerConfiguration.class})
@TestPropertySource(
        properties = {
                "dbaas.api.retry.default.template.maxAttempts=1",
                "dbaas.api.retry.default.template.backOffPeriod=0"
        }
)
public class DbaasPostgresDataSourceTest {

    @Autowired
    private DataSource postgresDataSource;

    @Test
    public void testPostgresDataSource() throws SQLException {
        Assertions.assertTrue(postgresDataSource.isWrapperFor(HikariDataSource.class));
        try (Connection connection = postgresDataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT datname FROM pg_database WHERE datistemplate = false;")) {
            // select the list of databases
            ResultSet resultSet = preparedStatement.executeQuery();
            boolean next = resultSet.next();
            Assertions.assertTrue(next);
        }
    }
}
