package com.netcracker.cloud.dbaas.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.qubership.cloud.dbaas.client.config.DbaasClickhouseConfiguration.*;

@SpringBootTest
@ContextConfiguration(classes = {ClickhouseTestContainerConfiguration.class,
        ClickhouseTestConfiguration.class})
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true"})
class ClickhouseTest {

    @Autowired
    @Qualifier(SERVICE_CLICKHOUSE_DATASOURCE)
    private DataSource clickhouseDatasource;

    @Test
     void testClickhouseDataSource() throws SQLException {
        try (Connection connection = clickhouseDatasource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1;")) {
            preparedStatement.executeQuery();
        }
    }
}
