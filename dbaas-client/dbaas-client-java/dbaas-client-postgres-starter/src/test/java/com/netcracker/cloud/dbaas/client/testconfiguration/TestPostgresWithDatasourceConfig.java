package com.netcracker.cloud.dbaas.client.testconfiguration;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.config.EnableDbaasPostgresql;
import com.netcracker.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import com.netcracker.cloud.dbaas.client.entity.database.PostgresDatabase;
import com.netcracker.cloud.dbaas.client.entity.database.type.PostgresDBType;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.netcracker.cloud.dbaas.client.DbaasConst.ADMIN_ROLE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Configuration
@EnableDbaasPostgresql
public class TestPostgresWithDatasourceConfig {
    int UNNECESSARY_URL_PARAMS_AMOUNT = 20;

    @Autowired
    @Qualifier("pgContainer")
    private PostgresqlContainerConfiguration container;

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

        when(dbaasClient.getOrCreateDatabase(any(PostgresDBType.class), anyString(), anyMap(), any(DatabaseConfig.class)))
                .thenAnswer((Answer<PostgresDatabase>) invocationOnMock -> {
                    HashMap<String, String> classifierFromMock = (HashMap<String, String>) invocationOnMock.getArguments()[2];
                    return getPostgresDb(classifierFromMock);
                });

        return dbaasClient;
    }

    public PostgresDatabase getPostgresDb(HashMap<String, String> classifier) {
        PostgresDatabase database = new PostgresDatabase();
        database.setName("test_db");

        String address = container.getJdbcUrl().substring(0, container.getJdbcUrl().length() - UNNECESSARY_URL_PARAMS_AMOUNT) + "test_db";
        PostgresDBConnection connection = new PostgresDBConnection(address,
                container.getUsername(),
                container.getPassword(),
                ADMIN_ROLE);
        database.setConnectionProperties(connection);
        SortedMap<String, Object> dbClassifier = new TreeMap<>(classifier);
        database.setClassifier(dbClassifier);

        return database;
    }
}
