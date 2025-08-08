package com.netcracker.cloud.dbaas.client.it.postgresql;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.config.EnableDbaasPostgresql;
import com.netcracker.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import com.netcracker.cloud.dbaas.client.entity.database.PostgresDatabase;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;

import static com.netcracker.cloud.dbaas.client.DbaasConst.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Configuration
@EnableDbaasPostgresql
@Slf4j
public class TestPostgresWithDatasourceConfig {
    int UNNECESSARY_URL_PARAMS_AMOUNT = 20;

    @Autowired
    @Qualifier("pgContainer")
    private PostgresqlContainerConfiguration container;

    @Bean
    @Primary
    public DbaasClient dbaasClient() throws Exception {
        System.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
        DbaasClient dbaasClient = Mockito.mock(DbaasClient.class);

        when(dbaasClient.getOrCreateDatabase(any(), any(), any(), any(DatabaseConfig.class)))
                .thenAnswer((Answer<PostgresDatabase>) invocationOnMock -> {
                    HashMap<String, String> classifierFromMock = (HashMap<String, String>) invocationOnMock.getArguments()[2];
                    String databaseName = classifierFromMock.get(SCOPE).equals(SERVICE) ? "test_service" : classifierFromMock.get(TENANT_ID);
                    return getPostgresDb(databaseName);
                });

        return dbaasClient;
    }

    public PostgresDatabase getPostgresDb(String dbName) {

        PostgresDatabase database = new PostgresDatabase();
        database.setName(dbName);

        PostgresDBConnection connection = new PostgresDBConnection();
        String address = container.getJdbcUrl().substring(0, container.getJdbcUrl().length() - UNNECESSARY_URL_PARAMS_AMOUNT);
        connection.setUrl(address + dbName);
        connection.setUsername(container.getUsername());
        connection.setPassword(container.getPassword());
        database.setConnectionProperties(connection);

        return database;
    }
}
