package com.netcracker.cloud.dbaas.client;

import org.qubership.cloud.dbaas.client.config.EnableDbaasPostgresql;
import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.PostgresDBType;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.DbaasPostgresProxyDataSource;
import org.qubership.cloud.dbaas.client.management.classifier.DbaasClassifierFactory;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import com.zaxxer.hikari.HikariDataSource;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.qubership.cloud.dbaas.client.DbaasConst.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Configuration
@EnableDbaasPostgresql
@EnableFlywayPostgresql
public class TestFlywayConfig {
    int UNNECESSARY_URL_PARAMS_AMOUNT = 20;

    public final String SERVICE_POSTGRES_VERSIONED_DATASOURCE = "serviceVersionedDatasource";


    @Autowired
    @Qualifier("pgContainer")
    private PostgresqlContainerConfiguration container;

    @Bean(name = {SERVICE_POSTGRES_VERSIONED_DATASOURCE})
    public DataSource servicePostgresDatasource(DatabasePool databasePool,
                                                DbaasClassifierFactory classifierFactory,
                                                DbaasApiProperties postgresDbaasApiProperties) {
        DatabaseConfig databaseConfig = DatabaseConfig.builder()
                .userRole(postgresDbaasApiProperties.getRuntimeUserRole())
                .dbNamePrefix(postgresDbaasApiProperties.getDbPrefix())
                .build();

        DbaasPostgresProxyDataSource dataSource = new DbaasPostgresProxyDataSource(databasePool,
                classifierFactory.newServiceClassifierBuilder().withCustomKey(LOGICAL_DB_NAME, "configs"),
                databaseConfig);

        HikariDataSource hikariDataSource = (HikariDataSource) dataSource.getInnerDataSource();
        hikariDataSource.setMaximumPoolSize(20);
        return dataSource;
    }

    @Bean
    @Primary
    @Qualifier("dbaasRestClient")
    public MicroserviceRestClient mockDbaasRestClient() {
        return Mockito.mock(MicroserviceRestClient.class);
    }

    @Bean
    @Primary
    public DbaasClient getDbaasClient() {
        DbaasClient dbaasClient = Mockito.mock(DbaasClient.class);

        when(dbaasClient.getOrCreateDatabase(any(PostgresDBType.class), anyString(), anyMap(), any(DatabaseConfig.class)))
                .thenAnswer((Answer<PostgresDatabase>) invocationOnMock -> {
                    Map<String, Object> classifierFromMock = (HashMap<String, Object>) invocationOnMock.getArguments()[2];
                    return getPostgresDb(new TreeMap<>(classifierFromMock));
                });

        return dbaasClient;
    }

    public PostgresDatabase getPostgresDb(SortedMap<String, Object> classifier) {
        PostgresDatabase database = new PostgresDatabase();
        String name = "static_db";
        if (classifier.containsKey(CUSTOM_KEYS)) {
            name = "versioned_db";
        }
        database.setName(name);
        database.setClassifier(classifier);

        String address = container.getJdbcUrl().substring(0, container.getJdbcUrl().length() - UNNECESSARY_URL_PARAMS_AMOUNT) + name;
        PostgresDBConnection connection = new PostgresDBConnection(address, container.getUsername(), container.getPassword(), ADMIN_ROLE);
        database.setConnectionProperties(connection);

        return database;
    }
}
