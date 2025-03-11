package org.qubership.cloud.quarkus.dbaas.opensearch.client.it;

import org.qubership.cloud.framework.contexts.tenant.TenantProvider;
import org.qubership.cloud.framework.contexts.tenant.context.TenantContext;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.dbaas.client.opensearch.DbaasOpensearchClient;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.DbaasOpensearchConfiguration;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.it.configuration.ContainerLogicalDbProvider;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.transport.endpoints.BooleanResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


@TestProfile(ServiceNativeClientTest.TestProfileWithPrefix.class)
@QuarkusTest
@Slf4j
public class MultiTenancyNativeTest {
    String FIRST_TENANT = "a";
    String SECOND_TENANT = "b";
    String CREATION_TENANT = "tenant-test-id";
    String CREATION_TENANT_PREFIX = "test-tenant-test-id-prefix";

    @Inject
    @Named(DbaasOpensearchConfiguration.TENANT_NATIVE_OPENSEARCH_CLIENT)
    DbaasOpensearchClient tenantNativeClient;

    @BeforeAll
    public static void initContext() {
        ContextManager.register(Collections.singletonList(new TenantProvider()));
    }

    @Test
    public void testIndexWtihPrefix() throws IOException {
        TenantContext.set(CREATION_TENANT);
        CreateIndexRequest request = new CreateIndexRequest.Builder().index(tenantNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX)).build();
        tenantNativeClient.getClient().indices().create(request);
        assertEquals(CREATION_TENANT_PREFIX + "__" + ContainerLogicalDbProvider.TEST_INDEX,
                tenantNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX));

        ExistsRequest getRequest = new ExistsRequest.Builder().index(tenantNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX)).build();
        BooleanResponse indexExists = tenantNativeClient.getClient().indices().exists(getRequest);
        assertTrue(indexExists.value());
    }

    @Test
    void tenantClientMustConnectToTenantDb() throws IOException {
        TenantContext.set("tenant-id");
        IndexRequest<Map<String, String>> updateIndexRequest = new IndexRequest.Builder<Map<String, String>>()
                .index(tenantNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX))
                .id("1")
                .document(Map.of("key", "value"))
                .build();
        IndexResponse indexResponse = tenantNativeClient.getClient().index(updateIndexRequest);
        assertNotNull(indexResponse);

        GetRequest getRequest = new GetRequest.Builder().index(tenantNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX)).id("1").build();
        GetResponse<Map> getResponse = tenantNativeClient.getClient().get(getRequest, Map.class);
        assertNotNull(getResponse);
        assertTrue(getResponse.found());
        assertEquals("1", getResponse.id());
        Map<String, String> responseBody = getResponse.source();
        assertEquals(1, responseBody.size());
        assertEquals("value", responseBody.get("key"));
    }

    @Test
    void tenantClientMustConnectToRightTenantDb() throws IOException {
        String key = "key";
        TenantContext.set(FIRST_TENANT);
        IndexRequest<Map<String, String>> firstIndexRequest = new IndexRequest.Builder<Map<String, String>>()
                .index(tenantNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX))
                .id("1")
                .document(Map.of(key, FIRST_TENANT))
                .build();
        IndexResponse firstResponse = tenantNativeClient.getClient().index(firstIndexRequest);
        assertNotNull(firstResponse);

        TenantContext.set(SECOND_TENANT);
        IndexRequest<Map<String, String>> secondIndexRequest = new IndexRequest.Builder<Map<String, String>>()
                .index(tenantNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX))
                .id("1")
                .document(Map.of(key, SECOND_TENANT))
                .build();
        IndexResponse secondResponse = tenantNativeClient.getClient().index(secondIndexRequest);
        assertNotNull(secondResponse);

        TenantContext.set(FIRST_TENANT);
        GetRequest getRequest = new GetRequest.Builder().index(tenantNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX)).id("1").build();
        GetResponse<Map> getResponse = tenantNativeClient.getClient().get(getRequest, Map.class);
        assertNotNull(getResponse);
        assertTrue(getResponse.found());
        Map<String, String> responseBody = getResponse.source();
        assertEquals(FIRST_TENANT, responseBody.get(key));
    }

}
