package org.qubership.cloud.dbaas.client.management;

import com.clickhouse.jdbc.ClickHouseConnection;
import com.clickhouse.jdbc.ClickHouseDataSource;
import org.qubership.cloud.dbaas.client.entity.connection.ClickhouseConnection;
import org.qubership.cloud.dbaas.client.entity.database.ClickhouseDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.ClickhouseDBType;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class DbaasClickhouseDatasourceTest {

    @Mock
    DatabaseConfig databaseConfig;
    @Mock
    DatabasePool databasePool;
    @Mock
    DbaaSClassifierBuilder builder;

    @InjectMocks
    DbaasClickhouseDatasource dataSource;

    @Test
    void test() throws SQLException {
        DbaasDbClassifier dbaasDbClassifier = Mockito.mock(DbaasDbClassifier.class);
        Mockito.when(builder.build()).thenReturn(dbaasDbClassifier);
        ClickhouseDatabase clickhouseDatabase = Mockito.mock(ClickhouseDatabase.class);
        Mockito.when(databasePool.getOrCreateDatabase(ClickhouseDBType.INSTANCE, dbaasDbClassifier, databaseConfig, null)).thenReturn(clickhouseDatabase);
        ClickhouseConnection clickhouseConnection = Mockito.mock(ClickhouseConnection.class);
        Mockito.when(clickhouseDatabase.getConnectionProperties()).thenReturn(clickhouseConnection);
        ClickHouseDataSource ds = Mockito.mock(ClickHouseDataSource.class);
        Mockito.when(clickhouseConnection.getDataSource()).thenReturn(ds);
        SQLException sqlE = Mockito.mock(SQLException.class);
        AtomicInteger counter = new AtomicInteger(0);
        Mockito.when(ds.getConnection()).thenAnswer(i -> {
            if (counter.getAndIncrement() == 0) {
                throw sqlE;
            } else {
                return Mockito.mock(ClickHouseConnection.class);
            }
        });
        Mockito.when(sqlE.getSQLState()).thenReturn("28P01");

        dataSource.getConnection();

        Mockito.verify(databasePool).removeCachedDatabase(Mockito.eq(ClickhouseDBType.INSTANCE), Mockito.any(DbaasDbClassifier.class));
        Mockito.verify(databasePool).getOrCreateDatabase(Mockito.eq(ClickhouseDBType.INSTANCE), Mockito.any(DbaasDbClassifier.class));
    }
}