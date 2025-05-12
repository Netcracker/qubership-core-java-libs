package org.qubership.cloud.quarkus.dbaas.cassandraclient;

import com.datastax.oss.driver.api.core.CqlSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.framework.contexts.tenant.DefaultTenantProvider;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.classifier.TenantClassifierBuilder;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.service.CassandraClientCreation;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.qubership.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;
import static org.qubership.cloud.quarkus.dbaas.cassandraclient.CommonTestMethods.prepareCassandraDBConnection;

class TenantCassandraClientTest {

    private static DbaaSCassandraClient dbaaSCassandraClient;
    private static final CassandraClientCreation cassandraClientCreation = mock(CassandraClientCreation.class);
    private static CqlSession session = mock(CqlSession.class);
    private static CassandraDatabase cassandraDatabase;

    @BeforeAll
    static void init() {
        ContextManager.register(Collections.singletonList(new DefaultTenantProvider()));
    }

    @BeforeEach
    void prepare() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("dbClassifier", "default");
        dbaaSCassandraClient = new DbaaSCassandraClient(new TenantClassifierBuilder(params), cassandraClientCreation);
        CassandraDBConnection cassandraDBConnection = prepareCassandraDBConnection();
        cassandraDBConnection.setSession(session);

        cassandraDatabase = new CassandraDatabase();
        cassandraDatabase.setConnectionProperties(cassandraDBConnection);

        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test-tenant"));
    }

    @Test
    void mustReturnSameTenantCassandraSession() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("dbClassifier", "default");
        params.put("tenantId", ((TenantContextObject) ContextManager.get(TENANT_CONTEXT_NAME)).getTenant());
        DbaasDbClassifier classifier = new DbaasDbClassifier(params);
        SortedMap<String, Object> newMap = new TreeMap<>(classifier.asMap());
        cassandraDatabase.setClassifier(newMap);
        when(cassandraClientCreation.getOrCreateCassandraDatabase(any())).thenReturn(cassandraDatabase);

        CqlSession firstSession = dbaaSCassandraClient.getOrCreateCassandraDatabase().getConnectionProperties().getSession();
        assertNotNull(firstSession);

        CqlSession secondSession = dbaaSCassandraClient.getOrCreateCassandraDatabase().getConnectionProperties().getSession();
        assertEquals(firstSession, secondSession);
    }

    @Test
    void testTenantClassifierWithoutTenantId() {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject((String)null));
        try {
            dbaaSCassandraClient.getOrCreateCassandraDatabase();
        } catch (Exception e) {
            assertEquals("Tenant is not set", e.getMessage());
        }
    }
}
