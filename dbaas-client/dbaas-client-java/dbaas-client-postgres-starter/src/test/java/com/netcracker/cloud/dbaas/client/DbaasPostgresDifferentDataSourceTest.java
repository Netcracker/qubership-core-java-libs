package com.netcracker.cloud.dbaas.client;

import com.netcracker.cloud.dbaas.client.testconfiguration.PostgresTestContainerConfiguration;
import com.netcracker.cloud.dbaas.client.testconfiguration.TestPostgresWithDatasourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.SQLException;

@Slf4j
@SpringBootTest
@ContextConfiguration(classes = {TestPostgresWithDatasourceConfig.class, PostgresTestContainerConfiguration.class})
@TestPropertySource(
        properties = {
                "dbaas.api.retry.default.template.maxAttempts=1",
                "dbaas.api.retry.default.template.backOffPeriod=0",
                "dbaas.postgres.dataSourceType=org.apache.commons.dbcp2.BasicDataSource"
        }
)
public class DbaasPostgresDifferentDataSourceTest {

    @Autowired
    private DataSource postgresDataSource;

    @Test
    public void testPostgresDataSource() throws SQLException {
        Assertions.assertTrue(postgresDataSource.isWrapperFor(BasicDataSource.class));
    }
}
