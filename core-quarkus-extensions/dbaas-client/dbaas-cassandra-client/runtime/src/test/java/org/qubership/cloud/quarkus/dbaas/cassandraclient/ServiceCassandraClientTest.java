package org.qubership.cloud.quarkus.dbaas.cassandraclient;

import com.datastax.oss.driver.api.core.CqlSession;
import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.classifier.ServiceClassifierBuilder;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.service.CassandraClientCreation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.qubership.cloud.quarkus.dbaas.cassandraclient.CommonTestMethods.prepareCassandraDBConnection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceCassandraClientTest {

    private static DbaaSCassandraClient dbaaSCassandraClient;
    private static final CassandraClientCreation cassandraClientCreation = mock(CassandraClientCreation.class);
    private static CqlSession session = mock(CqlSession.class);
    private static CassandraDatabase cassandraDatabase;

    @BeforeEach
    void prepare() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("dbClassifier", "default");
        params.put(SCOPE, SERVICE);
        dbaaSCassandraClient = new DbaaSCassandraClient(new ServiceClassifierBuilder(params), cassandraClientCreation);
        CassandraDBConnection cassandraDBConnection = prepareCassandraDBConnection();
        cassandraDBConnection.setSession(session);

        cassandraDatabase = new CassandraDatabase();
        cassandraDatabase.setConnectionProperties(cassandraDBConnection);
    }

    @Test
    void mustReturnSameServiceCassandraSession() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("dbClassifier", "default");
        params.put(SCOPE, SERVICE);
        DbaasDbClassifier classifier = new DbaasDbClassifier(params);
        SortedMap<String, Object> newMap = new TreeMap<>(classifier.asMap());

        cassandraDatabase.setClassifier(newMap);
        when(cassandraClientCreation.getOrCreateCassandraDatabase(any())).thenReturn(cassandraDatabase);

        CqlSession firstSession = dbaaSCassandraClient.getOrCreateCassandraDatabase().getConnectionProperties().getSession();
        assertNotNull(firstSession);

        CqlSession secondSession = dbaaSCassandraClient.getOrCreateCassandraDatabase().getConnectionProperties().getSession();
        assertEquals(firstSession, secondSession);
    }
}
