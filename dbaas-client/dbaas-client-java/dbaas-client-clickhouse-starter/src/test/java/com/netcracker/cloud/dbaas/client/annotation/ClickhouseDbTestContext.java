package com.netcracker.cloud.dbaas.client.annotation;

import com.clickhouse.jdbc.ClickHouseConnection;
import com.clickhouse.jdbc.ClickHouseDataSource;
import com.netcracker.cloud.dbaas.client.config.annotation.EnableDbaasClickhouse;
import com.netcracker.cloud.dbaas.client.config.MSInfoProvider;
import com.netcracker.cloud.dbaas.client.entity.connection.ClickhouseConnection;
import com.netcracker.cloud.dbaas.client.entity.database.ClickhouseDatabase;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;
import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.sql.PreparedStatement;
import java.util.TreeMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@EnableDbaasClickhouse
public class ClickhouseDbTestContext {

    @Primary
    @Bean
    @Qualifier("dbaasRestClient")
    public static MicroserviceRestClient microserviceRestClient() {
        return Mockito.mock(MicroserviceRestClient.class);
    }

    @Primary
    @Bean
    public MSInfoProvider msInfoProvider() {
        return Mockito.mock(MSInfoProvider.class);
    }

    /**
     * This {@code DatabasePool} stub is required to return fake {@code ClickhouseDatabase}
     * instance, but with real DB classifier.
     *
     * @return fake {@code ClickhouseDatabase} instance with real DB classifier.
     */
    @Primary
    @Bean
    public DatabasePool dbaasConnectionPool() {
        DatabasePool databasePool = Mockito.mock(DatabasePool.class);
        when(databasePool.getOrCreateDatabase(any(), any(), any(), any())).then(invocationOnMock -> {
            // Getting DB classifier from argument 1
            DbaasDbClassifier classifier = invocationOnMock.getArgument(1);
            // Creating fake Clickhouse DB with classifier from original method invocation arguments
            ClickhouseDatabase db = new ClickhouseDatabase();
            db.setClassifier(new TreeMap<>(classifier.asMap()));
            ClickhouseConnection clickhouseConnection = new ClickhouseConnection();
            ClickHouseDataSource dataSource = mock(ClickHouseDataSource.class);
            ClickHouseConnection connection = mock(ClickHouseConnection.class);
            when(connection.prepareStatement(anyString())).thenReturn(mock(PreparedStatement.class));
            when(dataSource.getConnection()).thenReturn(connection);
            clickhouseConnection.setDataSource(dataSource);
            db.setConnectionProperties(clickhouseConnection);
            return db;
        });
        return databasePool;
    }
}
