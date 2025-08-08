package com.netcracker.cloud.dbaas.client.management;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.config.annotation.EnableDbaasClickhouse;
import com.netcracker.cloud.dbaas.client.entity.connection.ClickhouseConnection;
import com.netcracker.cloud.dbaas.client.entity.database.ClickhouseDatabase;
import com.netcracker.cloud.dbaas.client.entity.database.type.ClickhouseDBType;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.netcracker.cloud.dbaas.client.DbaasConst.ADMIN_ROLE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Configuration
@EnableDbaasClickhouse
public class TestClickhouseConfig {
    @Bean
    @Primary
    @Qualifier("dbaasRestClient")
    public MicroserviceRestClient mockDbaasRestClient() {
        return Mockito.mock(MicroserviceRestClient.class);
    }

    @Bean
    @Primary
    public DbaasClient dbaasClientMock() throws Exception {
        DbaasClient dbaasClient = Mockito.mock(DbaasClient.class);

        when(dbaasClient.getOrCreateDatabase(any(ClickhouseDBType.class), anyString(), anyMap(), any(DatabaseConfig.class)))
                .thenAnswer((Answer<ClickhouseDatabase>) invocationOnMock -> {
                    HashMap<String, String> classifierFromMock = (HashMap<String, String>) invocationOnMock.getArguments()[2];
                    return getClickhouseDb(classifierFromMock);
                });

        return dbaasClient;
    }

    public ClickhouseDatabase getClickhouseDb(HashMap<String, String> classifier) throws IOException {
        ClickhouseDatabase database = new ClickhouseDatabase();
        database.setName("clickhouse");

        ClickhouseConnection connection = new ClickhouseConnection("jdbc:clickhouse://localhost:5432/clickhouse", "admin", "admin", ADMIN_ROLE);
        database.setConnectionProperties(connection);
        SortedMap<String, Object> dbClassifier = new TreeMap<>(classifier);
        database.setClassifier(dbClassifier);

        return database;
    }
}
