package com.netcracker.cloud.dbaas.client.opensearch.restclient;

import com.netcracker.cloud.dbaas.client.opensearch.DbaasOpensearchClient;
import com.netcracker.cloud.dbaas.client.opensearch.restclient.configuration.OpensearchTestConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.AcknowledgedResponseBase;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.mapping.FieldMapping;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TextProperty;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryStringQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermQuery;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.indices.*;
import org.opensearch.client.opensearch.indices.get_alias.IndexAliases;
import org.opensearch.client.opensearch.indices.get_field_mapping.TypeFieldMappings;
import org.opensearch.client.opensearch.indices.get_index_template.IndexTemplateItem;
import org.opensearch.client.opensearch.indices.get_mapping.IndexMappingRecord;
import org.opensearch.client.opensearch.indices.put_index_template.IndexTemplateMapping;
import org.opensearch.client.opensearch.indices.update_aliases.Action;
import com.netcracker.cloud.dbaas.client.opensearch.config.DbaasOpensearchConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static com.netcracker.cloud.dbaas.client.opensearch.restclient.configuration.OpensearchTestConfiguration.*;

@SpringBootTest
@ContextConfiguration(classes = {OpensearchTestConfiguration.class})
@TestPropertySource("classpath:application.properties")
@Slf4j
public class DbaasIndicesClientTest {

    @Autowired
    @Qualifier(DbaasOpensearchConfiguration.SERVICE_NATIVE_OPENSEARCH_CLIENT)
    private DbaasOpensearchClient serviceClient;

    private String prefixWithDelimiter;
    private String fullIndexName;

    @BeforeEach
    public void setUp() throws IOException {
        prefixWithDelimiter = TEST_PREFIX + "_";
        fullIndexName = TEST_FULL_INDEX_NAME;
        clear(TEST_INDEX);
        clearTemplate(TEST_TEMPLATE);
    }

    public void clear(String index) throws IOException {
        try {
            serviceClient.getClient().indices().delete(new DeleteIndexRequest.Builder().index(serviceClient.normalize(index)).build());
        } catch (OpenSearchException e) {
            log.info("Nothing to delete");
        }
    }

    public void clearTemplate(String template) throws IOException {
        try {
            serviceClient.getClient().indices().deleteTemplate(new DeleteTemplateRequest.Builder().name(serviceClient.normalize(template)).build());
        } catch (OpenSearchException e) {
            log.info("Nothing to delete");
        }
        try {
            serviceClient.getClient().indices().deleteIndexTemplate(new DeleteIndexTemplateRequest.Builder().name(serviceClient.normalize(template)).build());
        } catch (OpenSearchException e) {
            log.info("Nothing to delete");
        }
    }

    @Test
    public void delete() throws IOException {
        createIndex(TEST_INDEX);
        AcknowledgedResponseBase deleteIndexResponse = serviceClient.getClient().indices().delete(
                new DeleteIndexRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).build());
        assertTrue(deleteIndexResponse.acknowledged());
        boolean indexExists = serviceClient.getClient().indices().exists(builder -> builder.index(serviceClient.normalize(TEST_INDEX))).value();
        assertFalse(indexExists);
    }

    @Test
    public void create() throws IOException {
        CreateIndexResponse createIndexResponse = serviceClient.getClient().indices().create(builder -> builder.index(serviceClient.normalize(TEST_INDEX)));
        assertEquals(fullIndexName, createIndexResponse.index());
    }

    @Test
    public void putMapping() throws IOException {
        createIndex(TEST_INDEX);
        Map<String, Property> properties = new HashMap<>();
        properties.put("message", new Property.Builder().text(textPropertyBuilder -> textPropertyBuilder).build());
        PutMappingRequest putMappingRequest = new PutMappingRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .properties(properties)
                .build();
        AcknowledgedResponseBase putMappingResponse = serviceClient.getClient().indices().putMapping(putMappingRequest);
        assertTrue(putMappingResponse.acknowledged());
    }

    @Test
    public void getMapping() throws IOException {
        createIndex(TEST_INDEX);
        Map<String, Property> properties = new HashMap<>();
        properties.put("message", new Property.Builder().text(textPropertyBuilder -> textPropertyBuilder).build());
        PutMappingRequest putMappingRequest = new PutMappingRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .properties(properties)
                .build();
        AcknowledgedResponseBase putMappingResponse = serviceClient.getClient().indices().putMapping(putMappingRequest);
        assertTrue(putMappingResponse.acknowledged());

        GetMappingResponse getMappingResponse = serviceClient.getClient().indices().getMapping(builder -> builder.index(serviceClient.normalize(TEST_INDEX)));
        IndexMappingRecord indexMapping = getMappingResponse.get(fullIndexName);
        Map<String, Property> actualProperties = indexMapping.mappings().properties();
        assertEqualProperties(actualProperties, properties);
    }

    private static void assertEqualProperties(Map<String, Property> expectedProperties, Map<String, Property> actualProperties) {
        assertEquals(1, actualProperties.size());
        assertEquals(expectedProperties.keySet(), actualProperties.keySet());
        assertTrue(actualProperties.get("message").isText());
    }

    @Test
    public void getFieldMapping() throws IOException {
        createIndex(TEST_INDEX);
        Map<String, Property> properties = new HashMap<>();
        properties.put("message", new Property.Builder().text(textPropertyBuilder -> textPropertyBuilder).build());
        PutMappingRequest putMappingRequest = new PutMappingRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .properties(properties)
                .build();
        AcknowledgedResponseBase putMappingResponse = serviceClient.getClient().indices().putMapping(putMappingRequest);
        assertTrue(putMappingResponse.acknowledged());

        GetFieldMappingRequest request = new GetFieldMappingRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .fields("message")
                .build();
        GetFieldMappingResponse response = serviceClient.getClient().indices().getFieldMapping(request);

        TypeFieldMappings typeFieldMappings = response.get(fullIndexName);
        FieldMapping fieldMapping = typeFieldMappings.mappings().get("message");
        Map<String, Property> actualProperties = fieldMapping.mapping();
        assertEqualProperties(properties, actualProperties);
    }

    @Test
    public void updateAliases() throws IOException {
        createIndex(TEST_INDEX);
        String alias1 = "alias1";
        String alias2 = "alias2";

        Action aliasAction1 = new Action.Builder()
                .add(actionBuilder -> actionBuilder
                        .index(serviceClient.normalize(TEST_INDEX))
                        .alias(serviceClient.normalize(alias1)))
                .build();
        Action aliasAction2 = new Action.Builder()
                .add(actionBuilder -> actionBuilder
                        .indices(serviceClient.normalize(TEST_INDEX))
                        .alias(serviceClient.normalize(alias2))
                        .routing("1"))
                .build();
        UpdateAliasesRequest request = new UpdateAliasesRequest.Builder().actions(aliasAction1, aliasAction2).build();
        AcknowledgedResponseBase indicesAliasesResponse = serviceClient.getClient().indices().updateAliases(request);
        assertTrue(indicesAliasesResponse.acknowledged());

        GetIndexRequest getRequest = new GetIndexRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).build();
        GetIndexResponse getIndexResponse = serviceClient.getClient().indices().get(getRequest);
        Map<String, Alias> indexAliases = getIndexResponse.get(fullIndexName).aliases();
        assertEquals(2, indexAliases.size());
        assertTrue(indexAliases.containsKey(prefixWithDelimiter + alias1));
        assertTrue(indexAliases.containsKey(prefixWithDelimiter + alias2));
    }

    @Test
    public void open() throws IOException {
        createIndex(TEST_INDEX);
        OpenRequest request = new OpenRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).build();
        OpenResponse openIndexResponse = serviceClient.getClient().indices().open(request);
        assertTrue(openIndexResponse.acknowledged());
    }

    @Test
    public void close() throws IOException {
        createIndex(TEST_INDEX);
        CloseIndexRequest request = new CloseIndexRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).build();
        AcknowledgedResponseBase closeIndexResponse = serviceClient.getClient().indices().close(request);
        assertTrue(closeIndexResponse.acknowledged());
    }

    @Test
    public void existsAlias() throws IOException {
        createIndex(TEST_INDEX);
        String aliasName = "alias";

        Action aliasAction = new Action.Builder()
                .add(actionBuilder -> actionBuilder
                        .index(serviceClient.normalize(TEST_INDEX))
                        .alias(aliasName))
                .build();
        UpdateAliasesRequest request = new UpdateAliasesRequest.Builder().actions(aliasAction).build();
        AcknowledgedResponseBase indicesAliasesResponse = serviceClient.getClient().indices().updateAliases(request);
        assertTrue(indicesAliasesResponse.acknowledged());

        ExistsAliasRequest requestWithAlias = new ExistsAliasRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .name(aliasName)
                .build();
        boolean exists = serviceClient.getClient().indices().existsAlias(requestWithAlias).value();
        assertTrue(exists);
    }

    @Test
    public void refresh() throws IOException {
        createIndex(TEST_INDEX);
        RefreshRequest request = new RefreshRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).build();
        RefreshResponse refreshResponse = serviceClient.getClient().indices().refresh(request);
        assertEquals(0, refreshResponse.shards().failed());
    }

    @Test
    public void flush() throws IOException {
        createIndex(TEST_INDEX);
        FlushRequest request = new FlushRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).build();
        FlushResponse flushResponse = serviceClient.getClient().indices().flush(request);
        assertEquals(0, flushResponse.shards().failed());
    }

    @Test
    public void getSettings() throws IOException {
        createIndex(TEST_INDEX);
        GetIndicesSettingsRequest request = new GetIndicesSettingsRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).build();
        GetIndicesSettingsResponse getSettingsResponse = serviceClient.getClient().indices().getSettings(request);
        assertNotNull(getSettingsResponse);
        String numberOfReplicasString = getSettingsResponse.get(fullIndexName).settings().index().numberOfReplicas();
        assertEquals("1", numberOfReplicasString);
    }

    @Test
    public void get() throws IOException {
        createIndex(TEST_INDEX, TEST_ALIAS);
        GetIndexRequest request = new GetIndexRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).build();
        GetIndexResponse getIndexResponse = serviceClient.getClient().indices().get(request);
        Map<String, Alias> indexAliases = getIndexResponse.get(fullIndexName).aliases();
        assertEquals(1, indexAliases.size());
        assertTrue(indexAliases.containsKey(prefixWithDelimiter + TEST_ALIAS));
    }

    @Test
    public void forcemerge() throws IOException {
        createIndex(TEST_INDEX);
        ForcemergeRequest request = new ForcemergeRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).build();
        ForcemergeResponse forceMergeResponse = serviceClient.getClient().indices().forcemerge(request);
        assertEquals(0, forceMergeResponse.shards().failed());
    }

    @Test
    public void clearCache() throws IOException {
        createIndex(TEST_INDEX);
        ClearCacheRequest request = new ClearCacheRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).build();
        ClearCacheResponse clearCacheResponse = serviceClient.getClient().indices().clearCache(request);
        assertEquals(0, clearCacheResponse.shards().failed());
    }

    @Test
    public void exists() throws IOException {
        createIndex(TEST_INDEX);
        ExistsRequest requestExists = new ExistsRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).build();
        boolean expectedExists = serviceClient.getClient().indices().exists(requestExists).value();
        assertTrue(expectedExists);

        ExistsRequest requestDontExists = new ExistsRequest.Builder().index("some_idx").build();
        boolean expectedNotExists = serviceClient.getClient().indices().exists(requestDontExists).value();
        assertFalse(expectedNotExists);
    }

    @Test
    public void shrink() throws IOException {
        int initialAmountOfShards = 4;
        CreateIndexRequest request = new CreateIndexRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .settings(settingsBuilder -> settingsBuilder
                        .numberOfShards(String.valueOf(initialAmountOfShards))
                        .blocksWrite(true))
                .build();
        serviceClient.getClient().indices().create(request);

        String targetIdx = "target_idx_shrink";
        ShrinkRequest resizeRequest = new ShrinkRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .target(serviceClient.normalize(targetIdx))
                .build();
        ShrinkResponse resizeResponse = serviceClient.getClient().indices().shrink(resizeRequest);
        assertTrue(resizeResponse.acknowledged());

        GetIndicesSettingsRequest getIndicesSettingsRequest = new GetIndicesSettingsRequest.Builder().index(serviceClient.normalize(targetIdx)).build();
        GetIndicesSettingsResponse getSettingsResponse = serviceClient.getClient().indices().getSettings(getIndicesSettingsRequest);
        assertNotNull(getSettingsResponse);
        String numberOfShardsString = getSettingsResponse.get(prefixWithDelimiter + targetIdx).settings().index().numberOfShards();
        int newAmountOfShards = Integer.parseInt(numberOfShardsString);
        assertTrue(newAmountOfShards < initialAmountOfShards);
    }

    @Test
    public void split() throws IOException {
        int initialAmountOfShards = 1;
        CreateIndexRequest request = new CreateIndexRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .settings(settingsBuilder -> settingsBuilder
                        .numberOfShards(String.valueOf(initialAmountOfShards))
                        .blocksWrite(true))
                .build();
        serviceClient.getClient().indices().create(request);

        String targetIdx = "target_idx_split";
        int targetAmountOfShards = 4;
        SplitRequest resizeRequest = new SplitRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .target(serviceClient.normalize(targetIdx))
                .settings("index.number_of_shards", JsonData.of(targetAmountOfShards))
                .build();

        SplitResponse resizeResponse = serviceClient.getClient().indices().split(resizeRequest);
        assertTrue(resizeResponse.acknowledged());

        GetIndicesSettingsRequest getIndicesSettingsRequest = new GetIndicesSettingsRequest.Builder().index(serviceClient.normalize(targetIdx)).build();
        GetIndicesSettingsResponse getSettingsResponse = serviceClient.getClient().indices().getSettings(getIndicesSettingsRequest);
        assertNotNull(getSettingsResponse);
        String numberOfShardsString = getSettingsResponse.get(prefixWithDelimiter + targetIdx).settings().index().numberOfShards();
        int newAmountOfShards = Integer.parseInt(numberOfShardsString);
        assertEquals(targetAmountOfShards, newAmountOfShards);
    }

    @Test
    public void testClone() throws IOException {
        Map<String, String> indexData = Map.of("Key", "Value");
        IndexRequest<Map> updateIndexRequest = new IndexRequest.Builder<Map>()
                .index(serviceClient.normalize(TEST_INDEX))
                .id("1")
                .document(indexData)
                .build();
        serviceClient.getClient().index(updateIndexRequest);

        PutIndicesSettingsRequest settingsRequest = new PutIndicesSettingsRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .settings(settingsBuilder -> settingsBuilder.blocksWrite(true))
                .build();
        AcknowledgedResponseBase updateSettingsResponse = serviceClient.getClient().indices().putSettings(settingsRequest);
        assertTrue(updateSettingsResponse.acknowledged());

        String targetIndex = "target_idx_clone";
        CloneIndexRequest request = new CloneIndexRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .target(targetIndex)
                .build();

        CloneIndexResponse resizeResponse = serviceClient.getClient().indices().clone(request);
        assertTrue(resizeResponse.acknowledged());

        GetRequest getRequest = new GetRequest.Builder().index(targetIndex).id("1").build();
        GetResponse<Map> getResponse = serviceClient.getClient().get(getRequest, Map.class);
        assertTrue(getResponse.found());
        assertEquals("1", getResponse.id());
        Map responseBody = getResponse.source();
        assertEquals(1, responseBody.size());
        assertEquals("Value", responseBody.get("Key"));
    }

    @Test
    public void rollover() throws IOException {
        createIndex(TEST_INDEX, TEST_ALIAS);
        RolloverRequest request = new RolloverRequest.Builder()
                .alias(serviceClient.normalize(TEST_ALIAS))
                .newIndex(serviceClient.normalize(TEST_INDEX) + "2").build();
        RolloverResponse rolloverResponse = serviceClient.getClient().indices().rollover(request);
        assertEquals(fullIndexName + "2", rolloverResponse.newIndex());
        assertTrue(rolloverResponse.acknowledged());
        assertTrue(rolloverResponse.rolledOver());
        ExistsRequest getRequest = new ExistsRequest.Builder().index(serviceClient.normalize(TEST_INDEX) + "2").build();
        boolean indexExists = serviceClient.getClient().indices().exists(getRequest).value();
        assertTrue(indexExists);
    }

    @Test
    public void getAlias() throws IOException {
        String aliasName = "alias_get";
        createIndex(TEST_INDEX, aliasName);
        GetAliasRequest requestWithAlias = new GetAliasRequest.Builder().name(serviceClient.normalize(aliasName)).build();
        GetAliasResponse response = serviceClient.getClient().indices().getAlias(requestWithAlias);
        Map<String, IndexAliases> aliases = response.result();
        assertEquals(1, aliases.size());
        assertEquals(1, aliases.get(fullIndexName).aliases().size());
    }

    @Test
    public void putSettings() throws IOException {
        createIndex(TEST_INDEX);
        int numOfReplicas = 2;
        PutIndicesSettingsRequest request = new PutIndicesSettingsRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .settings(new IndexSettings.Builder()
                        .numberOfReplicas(String.valueOf(numOfReplicas)).build())
                .build();
        AcknowledgedResponseBase updateSettingsResponse = serviceClient.getClient().indices().putSettings(request);
        assertTrue(updateSettingsResponse.acknowledged());

        GetIndicesSettingsRequest getIndicesSettingsRequest = new GetIndicesSettingsRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).build();
        GetIndicesSettingsResponse getSettingsResponse = serviceClient.getClient().indices().getSettings(getIndicesSettingsRequest);
        assertNotNull(getSettingsResponse);
        String numberOfReplicasString = getSettingsResponse.get(fullIndexName).settings().index().numberOfReplicas();
        int newAmountOfReplicas = Integer.parseInt(numberOfReplicasString);
        assertEquals(numOfReplicas, newAmountOfReplicas);
    }

    @Test
    public void putTemplate() throws IOException {
        String firstPattern = TEST_INDEX;
        String secondPattern = "log-*";
        TypeMapping mappings = new TypeMapping.Builder()
                .properties("message", new Property.Builder().text(new TextProperty.Builder().build()).build()).build();
        PutTemplateRequest request = new PutTemplateRequest.Builder()
                .name(TEST_TEMPLATE)
                .indexPatterns(List.of(serviceClient.normalize(firstPattern), serviceClient.normalize(secondPattern)))
                .mappings(mappings)
                .order(0)
                .build();
        AcknowledgedResponseBase putTemplateResponse = serviceClient.getClient().indices().putTemplate(request);
        assertTrue(putTemplateResponse.acknowledged());

        createIndex(TEST_INDEX);

        GetMappingRequest getMappingRequest = new GetMappingRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).build();
        GetMappingResponse getMappingResponse = serviceClient.getClient().indices().getMapping(getMappingRequest);
        Map<String, Property> mapping = getMappingResponse.get(fullIndexName).mappings().properties();
        assertEqualProperties(mappings.properties(), mapping);
    }

    @Test
    public void putIndexTemplate() throws IOException {
        IndexSettings settings = new IndexSettings.Builder()
                .index(new IndexSettings.Builder()
                        .numberOfShards("3")
                        .numberOfReplicas("1").build()).build();
        String firstPattern = TEST_INDEX;
        String secondPattern = "log-*";
        PutIndexTemplateRequest request = new PutIndexTemplateRequest.Builder()
                .name(serviceClient.normalize(TEST_TEMPLATE))
                .indexPatterns(serviceClient.normalize(firstPattern), secondPattern)
                .template(new IndexTemplateMapping.Builder().settings(settings).build())
                .priority(0).build();

        AcknowledgedResponseBase putTemplateResponse = serviceClient.getClient().indices().putIndexTemplate(request);
        assertTrue(putTemplateResponse.acknowledged());

        createIndex(TEST_INDEX);

        GetIndexRequest getIndexRequest = GetIndexRequest.of(builder -> builder.index(serviceClient.normalize(TEST_INDEX)));
        GetIndexResponse getIndexResponse = serviceClient.getClient().indices().get(getIndexRequest);
        assertEquals(settings.index().numberOfShards(), getIndexResponse.get(fullIndexName).settings().index().numberOfShards());
    }

    @Test
    public void validateQuery() throws IOException {
        Map<String, String> indexData = Map.of("Key", "Value");
        IndexRequest<Map<String, String>> updateIndexRequest = new IndexRequest.Builder<Map<String, String>>()
                .index(serviceClient.normalize(TEST_INDEX))
                .id("1")
                .document(indexData).build();
        serviceClient.getClient().index(updateIndexRequest);

        ValidateQueryRequest request = new ValidateQueryRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .query(new Query.Builder()
                        .bool(new BoolQuery.Builder()
                                .must(new Query.Builder().queryString(new QueryStringQuery.Builder().query("*:*").build()).build())
                                .filter(new Query.Builder().term(new TermQuery.Builder().field("Key").value(new FieldValue.Builder().stringValue("Value").build()).build()).build())
                                .build())
                        .build())
                .build();

        ValidateQueryResponse response = serviceClient.getClient().indices().validateQuery(request);
        assertTrue(response.valid());
    }

    @Test
    public void getIndexTemplate() throws IOException {
        String firstPattern = TEST_INDEX;
        String secondPattern = "log-*";
        PutIndexTemplateRequest putRequest = new PutIndexTemplateRequest.Builder()
                .name(serviceClient.normalize(TEST_TEMPLATE))
                .indexPatterns(firstPattern, secondPattern)
                .template(new IndexTemplateMapping.Builder()
                        .mappings(new TypeMapping.Builder()
                                .properties("message", new Property.Builder().text(new TextProperty.Builder().build()).build())
                                .build())
                        .build())
                .build();
        AcknowledgedResponseBase putTemplateResponse = serviceClient.getClient().indices().putIndexTemplate(putRequest);
        assertTrue(putTemplateResponse.acknowledged());

        GetIndexTemplateRequest request = new GetIndexTemplateRequest.Builder().name(serviceClient.normalize(TEST_TEMPLATE)).build();
        GetIndexTemplateResponse getTemplatesResponse = serviceClient.getClient().indices().getIndexTemplate(request);
        List<IndexTemplateItem> templates = getTemplatesResponse.indexTemplates();
        assertEquals(1, templates.size());
        Assertions.assertEquals(prefixWithDelimiter + TEST_TEMPLATE, templates.get(0).name());
    }

    @Test
    public void testGetIndexTemplate() throws IOException {
        String firstPattern = TEST_INDEX;
        String secondPattern = "log-*";
        PutIndexTemplateRequest request = new PutIndexTemplateRequest.Builder()
                .name(serviceClient.normalize(TEST_TEMPLATE))
                .indexPatterns(serviceClient.normalize(firstPattern), serviceClient.normalize(secondPattern))
                .template(new IndexTemplateMapping.Builder().settings(new IndexSettings.Builder()
                                .numberOfShards("3")
                                .numberOfReplicas("1")
                                .build())
                        .build())
                .priority(0)
                .build();

        AcknowledgedResponseBase putTemplateResponse = serviceClient.getClient().indices().putIndexTemplate(request);
        assertTrue(putTemplateResponse.acknowledged());
        GetIndexTemplateRequest getRequest = new GetIndexTemplateRequest.Builder().name(serviceClient.normalize(TEST_TEMPLATE)).build();
        GetIndexTemplateResponse getTemplatesResponse = serviceClient.getClient().indices().getIndexTemplate(getRequest);
        List<IndexTemplateItem> templates = getTemplatesResponse.indexTemplates();
        assertEquals(1, templates.size());
        Optional<IndexTemplateItem> template = templates.stream().filter(indexTemplateItem -> indexTemplateItem.name().equals(prefixWithDelimiter + TEST_TEMPLATE)).findFirst();
        assertTrue(template.isPresent());
        List<String> patterns = template.get().indexTemplate().indexPatterns();
        assertTrue(patterns.contains(fullIndexName));
        assertTrue(patterns.contains(prefixWithDelimiter + "log-*"));
    }

    @Test
    public void existsTemplate() throws IOException {
        String firstPattern = TEST_INDEX;
        String secondPattern = "log-*";
        PutTemplateRequest putRequest = new PutTemplateRequest.Builder()
                .name(TEST_TEMPLATE)
                .indexPatterns(firstPattern, secondPattern)
                .mappings(new TypeMapping.Builder()
                        .properties("Message", new Property.Builder().text(new TextProperty.Builder().build()).build())
                        .build())
                .build();
        AcknowledgedResponseBase putTemplateResponse = serviceClient.getClient().indices().putTemplate(putRequest);
        assertTrue(putTemplateResponse.acknowledged());

        ExistsTemplateRequest request = new ExistsTemplateRequest.Builder().name(TEST_TEMPLATE).build();
        boolean exists = serviceClient.getClient().indices().existsTemplate(request).value();
        assertTrue(exists);
    }

    @Test
    public void existsIndexTemplate() throws IOException {
        String firstPattern = TEST_INDEX;
        String secondPattern = "log-*";
        PutIndexTemplateRequest request = new PutIndexTemplateRequest.Builder()
                .name(serviceClient.normalize(TEST_TEMPLATE))
                .indexPatterns(firstPattern, secondPattern)
                .template(new IndexTemplateMapping.Builder().settings(new IndexSettings.Builder()
                                .numberOfShards("3")
                                .numberOfReplicas("1")
                                .build())
                        .build())
                .priority(0)
                .build();

        AcknowledgedResponseBase putTemplateResponse = serviceClient.getClient().indices().putIndexTemplate(request);
        assertTrue(putTemplateResponse.acknowledged());
        ExistsIndexTemplateRequest existsRequest = new ExistsIndexTemplateRequest.Builder().name(serviceClient.normalize(TEST_TEMPLATE)).build();
        boolean b = serviceClient.getClient().indices().existsIndexTemplate(existsRequest).value();
        assertTrue(b);
    }

    @Test
    public void deleteTemplate() throws IOException {
        String firstPattern = TEST_INDEX;
        String secondPattern = "log-*";
        PutTemplateRequest putRequest = new PutTemplateRequest.Builder()
                .name(TEST_TEMPLATE)
                .indexPatterns(firstPattern, secondPattern)
                .mappings(new TypeMapping.Builder()
                        .properties("message", new Property.Builder().text(new TextProperty.Builder().build()).build())
                        .build())
                .build();
        AcknowledgedResponseBase putTemplateResponse = serviceClient.getClient().indices().putTemplate(putRequest);
        assertTrue(putTemplateResponse.acknowledged());

        DeleteTemplateRequest request = new DeleteTemplateRequest.Builder().name(TEST_TEMPLATE).build();
        AcknowledgedResponseBase deleteTemplateAcknowledge = serviceClient.getClient().indices().deleteTemplate(request);
        assertTrue(deleteTemplateAcknowledge.acknowledged());
    }

    @Test
    public void deleteIndexTemplate() throws IOException {
        String firstPattern = TEST_INDEX;
        String secondPattern = "log-*";
        PutIndexTemplateRequest request = new PutIndexTemplateRequest.Builder()
                .name(serviceClient.normalize(TEST_TEMPLATE))
                .indexPatterns(firstPattern, secondPattern)
                .template(new IndexTemplateMapping.Builder().settings(new IndexSettings.Builder()
                                .numberOfShards("3")
                                .numberOfReplicas("1")
                                .build())
                        .build())
                .priority(0)
                .build();

        AcknowledgedResponseBase putTemplateResponse = serviceClient.getClient().indices().putIndexTemplate(request);
        assertTrue(putTemplateResponse.acknowledged());

        DeleteIndexTemplateRequest deleteRequest = new DeleteIndexTemplateRequest.Builder().name(serviceClient.normalize(TEST_TEMPLATE)).build();
        AcknowledgedResponseBase deleteTemplateAcknowledge = serviceClient.getClient().indices().deleteIndexTemplate(deleteRequest);
        assertTrue(deleteTemplateAcknowledge.acknowledged());

        ExistsIndexTemplateRequest existsRequest = new ExistsIndexTemplateRequest.Builder().name(TEST_TEMPLATE).build();
        boolean b = serviceClient.getClient().indices().existsIndexTemplate(existsRequest).value();
        assertFalse(b);
    }

    @Test
    public void deleteAlias() throws IOException {
        createIndex(TEST_INDEX, TEST_ALIAS);

        DeleteAliasRequest request = new DeleteAliasRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .name(serviceClient.normalize(TEST_ALIAS))
                .build();
        AcknowledgedResponseBase deleteAliasResponse = serviceClient.getClient().indices().deleteAlias(request);
        assertTrue(deleteAliasResponse.acknowledged());

        GetIndexRequest getRequest = new GetIndexRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).build();
        GetIndexResponse getIndexResponse = serviceClient.getClient().indices().get(getRequest);
        IndexState indexAliases = getIndexResponse.get(fullIndexName);
        assertEquals(0, indexAliases.aliases().size());
    }

    private CreateIndexResponse createIndex(String indexName, String aliasName) throws IOException {
        CreateIndexRequest.Builder createIndexRequestBuilder = new CreateIndexRequest.Builder()
                .index(serviceClient.normalize(indexName));
        if (aliasName != null) {
            createIndexRequestBuilder.aliases(serviceClient.normalize(aliasName), aliasBuilder -> aliasBuilder);
        }
        return serviceClient.getClient().indices().create(createIndexRequestBuilder.build());
    }

    private CreateIndexResponse createIndex(String indexName) throws IOException {
        return createIndex(indexName, null);
    }
}
