package com.netcracker.cloud.dbaas.client;

import com.netcracker.cloud.dbaas.client.config.annotation.EnableDbaasClickhouse;
import com.netcracker.cloud.dbaas.client.entity.connection.ClickhouseConnection;
import com.netcracker.cloud.dbaas.client.entity.database.ClickhouseDatabase;
import com.netcracker.cloud.dbaas.client.entity.database.type.ClickhouseDBType;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.testcontainers.clickhouse.ClickHouseContainer;

import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.netcracker.cloud.dbaas.client.ClickhouseTestContainer.*;
import static com.netcracker.cloud.dbaas.client.DbaasConst.ADMIN_ROLE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Configuration
@EnableDbaasClickhouse
class ClickhouseTestConfiguration {

    @Autowired
    @Qualifier("clickhouseContainer")
    ClickHouseContainer container;

    @Bean
    @Primary
    @Qualifier("dbaasRestClient")
    public MicroserviceRestClient mockDbaasRestClient() {
        return Mockito.mock(MicroserviceRestClient.class);
    }


    @Bean
    @Primary
    public DbaasClient getDbaasClient()  {
        DbaasClient dbaasClient = Mockito.mock(DbaasClient.class);

        when(dbaasClient.getOrCreateDatabase(any(ClickhouseDBType.class), anyString(), anyMap(), any(DatabaseConfig.class)))
                .thenAnswer((Answer<ClickhouseDatabase>) invocationOnMock -> {
                    HashMap<String, String> classifierFromMock = (HashMap<String, String>) invocationOnMock.getArguments()[2];
                    return getClickhouseDatabase(classifierFromMock);
                });

        return dbaasClient;
    }


    public ClickhouseDatabase getClickhouseDatabase(HashMap<String, String> classifier) {
        ClickhouseDatabase database = new ClickhouseDatabase();
        database.setName("test_db");

        String address = "jdbc:clickhouse://" + container.getHost() + ":" + container.getMappedPort(CLICKHOUSE_PORT) + "/" + CLICKHOUSE_ADMIN_DB;
        ClickhouseConnection connection = new ClickhouseConnection(address,
                container.getUsername(),
                container.getPassword(),
                ADMIN_ROLE);
        connection.setPort((int)(Math.random()*container.getMappedPort(CLICKHOUSE_PORT)));
        database.setConnectionProperties(connection);
        SortedMap<String, Object> dbClassifier = new TreeMap<>(classifier);
        database.setClassifier(dbClassifier);

        return database;
    }
}
