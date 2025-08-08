package com.netcracker.cloud.dbaas.client.config;

import org.qubership.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.TreeMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@EnableDbaasPostgresql
public class PostgresDbTestContext {

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
     * This {@code DatabasePool} stub is required to return fake {@code PostgresDatabase}
     * instance, but with real DB classifier.
     *
     * @return fake {@code PostgresDatabase} instance with real DB classifier.
     */
    @Primary
    @Bean
    public DatabasePool dbaasConnectionPool() {
        DatabasePool databasePool = Mockito.mock(DatabasePool.class);
        when(databasePool.getOrCreateDatabase(any(), any(), any(), any())).then(invocationOnMock -> {
            // Getting DB classifier from argument 1
            DbaasDbClassifier classifier = invocationOnMock.getArgument(1);
            // Creating fake Postgres DB with classifier from original method invocation arguments
            PostgresDatabase db = new PostgresDatabase();
            db.setClassifier(new TreeMap<>(classifier.asMap()));
            PostgresDBConnection postgresDBConnection = new PostgresDBConnection();
            DataSource dataSource = mock(DataSource.class);
            Connection connection = mock(Connection.class);
            when(connection.prepareStatement(anyString())).thenReturn(mock(PreparedStatement.class));
            when(dataSource.getConnection()).thenReturn(connection);
            postgresDBConnection.setDataSource(dataSource);
            db.setConnectionProperties(postgresDBConnection);
            return db;
        });
        return databasePool;
    }
}
