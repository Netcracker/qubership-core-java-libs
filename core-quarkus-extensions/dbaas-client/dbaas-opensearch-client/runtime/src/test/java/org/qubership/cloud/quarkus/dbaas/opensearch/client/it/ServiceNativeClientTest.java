package org.qubership.cloud.quarkus.dbaas.opensearch.client.it;

import org.qubership.cloud.dbaas.client.opensearch.DbaasOpensearchClient;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.DbaasOpensearchConfiguration;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.it.configuration.ContainerLogicalDbProvider;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.it.configuration.OpensearchContainerResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.indices.*;
import org.opensearch.client.opensearch.indices.get_mapping.IndexMappingRecord;
import org.opensearch.client.opensearch.indices.put_index_template.IndexTemplateMapping;
import org.opensearch.client.opensearch.indices.update_aliases.Action;
import org.opensearch.client.opensearch.indices.update_aliases.AddAction;
import org.opensearch.client.transport.endpoints.BooleanResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

@TestProfile(ServiceNativeClientTest.TestProfileWithPrefix.class)
@QuarkusTest
@Slf4j
class ServiceNativeClientTest {

    @Inject
    @Named(DbaasOpensearchConfiguration.SERVICE_NATIVE_OPENSEARCH_CLIENT)
    DbaasOpensearchClient serviceNativeClient;

    @BeforeEach
    public void setUp() throws Exception {
        clear();
    }

    public void clear() throws IOException {
        try {
            serviceNativeClient.getClient().indices().delete(new DeleteIndexRequest.Builder()
                    .index(serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX)).build());
        } catch (OpenSearchException e) {
            log.info("Index {} already hasn't exist", ContainerLogicalDbProvider.TEST_INDEX);
        }
    }

    @Test
    public void testIndexWtihPrefix() throws IOException {
        createIndex(serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX));
        assertEquals(TestProfileWithPrefix.DB_PREFIX_PROPERTY_VAL + "--" + ContainerLogicalDbProvider.TEST_INDEX, serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX));
        ExistsRequest getRequest = new ExistsRequest.Builder().index(serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX)).build();
        BooleanResponse indexExists = serviceNativeClient.getClient().indices().exists(getRequest);
        assertTrue(indexExists.value());
    }

    @Test
    public void testCreateIndexWithDocument() throws IOException {
        IndexRequest<Map<String, String>> updateIndexRequest = new IndexRequest.Builder<Map<String, String>>()
                .index(serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX))
                .id("1")
                .document(Map.of("key", "value"))
                .build();
        IndexResponse indexResponse = serviceNativeClient.getClient().index(updateIndexRequest);
        assertNotNull(indexResponse);

        GetRequest getRequest = new GetRequest.Builder().index(serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX)).id("1").build();
        GetResponse<Map> getResponse = serviceNativeClient.getClient().get(getRequest, Map.class);
        assertNotNull(getResponse);
        assertTrue(getResponse.found());
        assertEquals("1", getResponse.id());
        Map<String, String> responseBody = getResponse.source();
        assertEquals(1, responseBody.size());
        assertEquals("value", responseBody.get("key"));
    }

    @Test
    public void testCreateIndexWithAlias() throws IOException {
        CreateIndexResponse response = createIndex(serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX));
        String alias1 = "alias1";
        String alias2 = "alias2";
        UpdateAliasesRequest request = new UpdateAliasesRequest.Builder()
                .actions(new Action.Builder()
                                .add(new AddAction.Builder()
                                        .index(serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX))
                                        .alias(serviceNativeClient.normalize(alias1))
                                        .build())
                                .build(),
                        new Action.Builder()
                                .add(new AddAction.Builder()
                                        .indices(serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX))
                                        .alias(serviceNativeClient.normalize(alias2))
                                        .routing("1")
                                        .build())
                                .build()
                )
                .build();
        UpdateAliasesResponse indicesAliasesResponse = serviceNativeClient.getClient().indices().updateAliases(request);
        assertTrue(indicesAliasesResponse.acknowledged());

        GetIndexRequest getRequest = new GetIndexRequest.Builder().index(serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX)).build();
        GetIndexResponse getIndexResponse = serviceNativeClient.getClient().indices().get(getRequest);
        Map<String, Alias> indexAliases = getIndexResponse.result().get(serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX)).aliases();
        assertEquals(2, indexAliases.size());
        assertTrue(indexAliases.containsKey(TestProfileWithPrefix.DB_PREFIX_PROPERTY_VAL + "--" + alias1));
        assertTrue(indexAliases.containsKey(TestProfileWithPrefix.DB_PREFIX_PROPERTY_VAL + "--" + alias2));
    }

    @Test
    public void testCreateIndexWithTemplate() throws IOException {
        String firstPattern = serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX);
        String secondPattern = serviceNativeClient.normalize("log-*");
        Map<String, Property> properties =
                Map.of("message", new Property.Builder().text(textPropertyBuilder -> textPropertyBuilder).build());
        PutIndexTemplateRequest request = new PutIndexTemplateRequest.Builder()
                .name(serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_TEMPLATE))
                .indexPatterns(asList(firstPattern, secondPattern))
                .template(new IndexTemplateMapping.Builder()
                        .mappings(new TypeMapping.Builder()
                                .properties(properties)
                                .build())
                        .build())
                .build();
        PutIndexTemplateResponse putTemplateResponse = serviceNativeClient.getClient().indices().putIndexTemplate(request);
        assertTrue(putTemplateResponse.acknowledged());

        createIndex(serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX));

        GetMappingRequest getMappingRequest = new GetMappingRequest.Builder()
                .index(serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX))
                .build();
        GetMappingResponse getMappingResponse = serviceNativeClient.getClient().indices().getMapping(getMappingRequest);
        IndexMappingRecord allMappings = getMappingResponse.get(serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX));
        Map<String, Property> actualProperties = allMappings.mappings().properties();
        assertEqualProperties(properties, actualProperties);
    }

    private static void assertEqualProperties(Map<String, Property> expectedProperties, Map<String, Property> actualProperties) {
        assertEquals(1, actualProperties.size());
        assertEquals(expectedProperties.keySet(), actualProperties.keySet());
        assertTrue(actualProperties.get("message").isText());
    }

    @Test
    public void testIndexDeletion() throws IOException {
        createIndex(serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX));
        DeleteIndexResponse deleteIndexResponse = serviceNativeClient.getClient().indices().delete(
                new DeleteIndexRequest.Builder().index(serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX)).build());
        assertTrue(deleteIndexResponse.acknowledged());
        ExistsRequest existsRequest = new ExistsRequest.Builder().index(serviceNativeClient.normalize(ContainerLogicalDbProvider.TEST_INDEX)).build();
        BooleanResponse indexExists = serviceNativeClient.getClient().indices().exists(existsRequest);
        assertFalse(indexExists.value());
    }

    private CreateIndexResponse createIndex(String indexName, String aliasName) throws IOException {
        CreateIndexRequest.Builder request = new CreateIndexRequest.Builder().index(indexName);
        if (aliasName != null) {
            request.aliases(aliasName, new Alias.Builder().build());
        }
        return serviceNativeClient.getClient().indices().create(request.build());
    }

    private CreateIndexResponse createIndex(String indexName) throws IOException {
        return createIndex(indexName, null);
    }

    @NoArgsConstructor
    protected static final class TestProfileWithPrefix implements QuarkusTestProfile {
        protected static final String ROLE_PROPERTY_VAL = "admin";
        public static final String DB_NAME_SERVICE_PREFIX = "quarkus.dbaas.opensearch.api.service.prefix-config.prefix";
        public static final String DB_NAME_SERVICE_DELIMITER = "quarkus.dbaas.opensearch.api.service.prefix-config.delimiter";
        public static final String DB_NAME_TENANT_PREFIX = "quarkus.dbaas.opensearch.api.tenant.prefix-config.prefix";
        public static final String DB_NAME_TENANT_DELIMITER = "quarkus.dbaas.opensearch.api.tenant.prefix-config.delimiter";
        public static final String ServicePhysicalDatabase = "quarkus.dbaas.opensearch.api.service.physical-database-id";

        protected static final String DB_PREFIX_PROPERTY_VAL = "test-prefix";
        protected static final String DB_TENATN_PREFIX_PROPERTY_VAL = "test-{tenantId}-prefix";

        @Override
        public List<TestResourceEntry> testResources() {
            return Collections.singletonList(new TestResourceEntry(OpensearchContainerResource.class));
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> properties = new HashMap<>();
            properties.put(DB_NAME_SERVICE_PREFIX, DB_PREFIX_PROPERTY_VAL);
            properties.put(DB_NAME_SERVICE_DELIMITER, "--");
            properties.put(DB_NAME_TENANT_PREFIX, DB_TENATN_PREFIX_PROPERTY_VAL);
            properties.put(DB_NAME_TENANT_DELIMITER, "__");
            properties.put(ServicePhysicalDatabase, "1234");
            return properties;
        }

        @Override
        public boolean disableGlobalTestResources() {
            return true;
        }
    }
}
