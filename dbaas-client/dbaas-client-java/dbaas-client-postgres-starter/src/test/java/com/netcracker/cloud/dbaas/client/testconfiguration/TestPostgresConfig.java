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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.netcracker.cloud.dbaas.client.DbaasConst.ADMIN_ROLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Configuration
@EnableDbaasPostgresql
public class TestPostgresConfig {
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

        when(dbaasClient.getOrCreateDatabase(any(PostgresDBType.class), anyString(), anyMap(), any(DatabaseConfig.class)))
                .thenAnswer((Answer<PostgresDatabase>) invocationOnMock -> {
                    HashMap<String, String> classifierFromMock = (HashMap<String, String>) invocationOnMock.getArguments()[2];
                    return getPostgresDb(classifierFromMock);
                });

        return dbaasClient;
    }

    public PostgresDatabase getPostgresDb(HashMap<String, String> classifier) throws IOException {
        PostgresDatabase database = new PostgresDatabase();
        database.setName("postgres");

        PostgresDBConnection connection = new PostgresDBConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "postgres", ADMIN_ROLE);
        database.setConnectionProperties(connection);
        SortedMap<String, Object> dbClassifier = new TreeMap<>(classifier);
        database.setClassifier(dbClassifier);

        return database;
    }
}
