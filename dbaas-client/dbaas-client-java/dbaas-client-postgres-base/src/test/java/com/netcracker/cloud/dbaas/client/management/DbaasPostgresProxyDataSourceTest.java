package org.qubership.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.PostgresDBType;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class DbaasPostgresProxyDataSourceTest {

    @Mock
    DatabaseConfig databaseConfig;
    @Mock
    DatabasePool databasePool;
    @Mock
    DbaaSClassifierBuilder builder;

    @InjectMocks
    DbaasPostgresProxyDataSource dataSource;

    @Test
    void test() throws SQLException {
        DbaasDbClassifier dbaasDbClassifier = Mockito.mock(DbaasDbClassifier.class);
        Mockito.when(builder.build()).thenReturn(dbaasDbClassifier);
        PostgresDatabase postgresDatabase = Mockito.mock(PostgresDatabase.class);
        Mockito.when(databasePool.getOrCreateDatabase(PostgresDBType.INSTANCE, dbaasDbClassifier, databaseConfig, null)).thenReturn(postgresDatabase);
        PostgresDBConnection postgresDBConnection = Mockito.mock(PostgresDBConnection.class);
        Mockito.when(postgresDatabase.getConnectionProperties()).thenReturn(postgresDBConnection);
        DataSource ds = Mockito.mock(DataSource.class);
        Mockito.when(postgresDBConnection.getDataSource()).thenReturn(ds);
        SQLException sqlE = Mockito.mock(SQLException.class);
        AtomicInteger counter = new AtomicInteger(0);
        Mockito.when(ds.getConnection()).thenAnswer(i -> {
            if (counter.getAndIncrement() == 0) {
                throw sqlE;
            } else {
                return Mockito.mock(Connection.class);
            }
        });
        Mockito.when(sqlE.getSQLState()).thenReturn("28P01");

        dataSource.getConnection();

        Mockito.verify(databasePool).removeCachedDatabase(Mockito.eq(PostgresDBType.INSTANCE), Mockito.any(DbaasDbClassifier.class));
        Mockito.verify(databasePool).getOrCreateDatabase(Mockito.eq(PostgresDBType.INSTANCE), Mockito.any(DbaasDbClassifier.class));
    }
}
