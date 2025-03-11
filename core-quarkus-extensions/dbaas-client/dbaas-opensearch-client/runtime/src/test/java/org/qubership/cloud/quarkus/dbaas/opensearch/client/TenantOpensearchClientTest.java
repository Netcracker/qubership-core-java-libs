package org.qubership.cloud.quarkus.dbaas.opensearch.client;

import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.qubership.cloud.framework.contexts.tenant.TenantProvider;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import org.qubership.cloud.dbaas.common.classifier.DbaaSClassifierFactory;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.service.OpensearchDbaaSApiClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.qubership.cloud.framework.contexts.tenant.TenantProvider.TENANT_CONTEXT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantOpensearchClientTest {
    private static DbaasOpensearchClientImpl dbaaSOpensearchClient;
    private static final OpensearchDbaaSApiClient opensearchApiClient = mock(OpensearchDbaaSApiClient.class);
    private static OpenSearchClient client = mock(OpenSearchClient.class);
    private static OpensearchIndexConnection opensearchIdxConnection;
    private static DbaaSClassifierFactory factory = mock(DbaaSClassifierFactory.class);


    @BeforeAll
    static void init() {
        ContextManager.register(Collections.singletonList(new TenantProvider()));
    }

    @BeforeEach
    void prepare() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("dbClassifier", "default");
        when(factory.newTenantClassifierBuilder(params)).thenCallRealMethod();
        dbaaSOpensearchClient = new DbaasOpensearchClientImpl(factory.newTenantClassifierBuilder(params), opensearchApiClient, "_");
        opensearchIdxConnection = CommonTestMethods.prepareOpensearchConnection();
        opensearchIdxConnection.setOpenSearchClient(client);

        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test-tenant"));
    }

    @Test
    void mustReturnSameTenantOpensearchClient() {
        when(opensearchApiClient.getOpensearchIndex(any())).thenReturn(opensearchIdxConnection);
        when(opensearchApiClient.getOrCreateOpensearchIndex(any())).thenReturn(opensearchIdxConnection);


        OpenSearchClient firstClient = dbaaSOpensearchClient.getOrCreateIndex().getOpenSearchClient();
        assertNotNull(firstClient);

        OpenSearchClient secondClient = dbaaSOpensearchClient.getOrCreateIndex().getOpenSearchClient();
        assertEquals(firstClient, secondClient);
    }

    @Test
    void testTenantClassifierWithoutTenantId() {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject((String)null));
        try {
            dbaaSOpensearchClient.getOrCreateIndex();
        } catch (Exception e) {
            assertEquals("Tenant is not set", e.getMessage());
        }
    }
}