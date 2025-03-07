package org.qubership.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.entity.database.ClickhouseDatasourceCreator;
import org.qubership.cloud.dbaas.client.entity.connection.ClickhouseConnection;
import org.qubership.cloud.dbaas.client.entity.database.ClickhouseDatabase;
import org.qubership.cloud.dbaas.client.entity.database.DbaasClickhouseDatasourceProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.qubership.cloud.dbaas.client.DbaasConst.ADMIN_ROLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ContextConfiguration(classes = {TestClickhouseConfig.class})
@TestPropertySource(properties = {
        "dbaas.clickhouse.datasource_properties.key1=value1", "dbaas.clickhouse.datasource_properties.key2=value2"})
class ClickhouseDatasourceCreatorTest {
    public final static String DB_NAME = "dbName";
    public final static String DB_USER = "dbaas";
    public final static String DB_PASSWORD = "dbaas";
    public final static String CLICKHOUSE_TEST_URI = "jdbc:clickhouse://localhost/";

    @Autowired
    private ClickhouseDatasourceCreator clickhouseDatasourceCreator;
    @SpyBean
    DbaasClickhouseDatasourceProperties datasourceProperties;


    @Test
    void mustGetSpringProperties() throws IOException {
        ClickhouseDatabase clickhouseDatabase = new ClickhouseDatabase();
        clickhouseDatabase.setName(DB_NAME);
        ClickhouseConnection clickhouseConnection = new ClickhouseConnection(CLICKHOUSE_TEST_URI + DB_NAME, DB_USER, DB_PASSWORD, ADMIN_ROLE);
        clickhouseDatabase.setConnectionProperties(clickhouseConnection);

        ClickhouseDatasourceCreator creator = new ClickhouseDatasourceCreator(datasourceProperties);
        creator.create(clickhouseDatabase);

        verify(datasourceProperties, times(3)).getDatasourceProperties();
    }
}