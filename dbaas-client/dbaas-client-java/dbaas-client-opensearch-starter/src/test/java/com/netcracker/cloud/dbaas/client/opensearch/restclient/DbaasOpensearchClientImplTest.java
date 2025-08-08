package com.netcracker.cloud.dbaas.client.opensearch.restclient;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.*;
import org.opensearch.client.opensearch._types.query_dsl.MatchAllQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.cluster.OpenSearchClusterClient;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.opensearch.client.opensearch.core.field_caps.FieldCapability;
import org.opensearch.client.opensearch.core.get.GetResult;
import org.opensearch.client.opensearch.core.mget.MultiGetOperation;
import org.opensearch.client.opensearch.core.mget.MultiGetResponseItem;
import org.opensearch.client.opensearch.core.msearch.*;
import org.opensearch.client.opensearch.core.msearch_template.TemplateConfig;
import org.opensearch.client.opensearch.core.rank_eval.*;
import org.opensearch.client.opensearch.core.reindex.Destination;
import org.opensearch.client.opensearch.core.reindex.Source;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.opensearch.ingest.OpenSearchIngestClient;
import org.opensearch.client.opensearch.snapshot.OpenSearchSnapshotClient;
import org.opensearch.client.opensearch.tasks.OpenSearchTasksClient;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.opensearch.DbaasOpensearchClient;
import org.qubership.cloud.dbaas.client.opensearch.config.DbaaSOpensearchConfigurationProperty;
import org.qubership.cloud.dbaas.client.opensearch.config.DbaasOpensearchConfiguration;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchProperties;
import org.qubership.cloud.dbaas.client.opensearch.restclient.configuration.OpensearchTestConfiguration;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.qubership.cloud.dbaas.client.opensearch.restclient.configuration.OpensearchTestConfiguration.TEST_INDEX;
import static org.qubership.cloud.dbaas.client.opensearch.restclient.configuration.OpensearchTestConfiguration.TEST_PREFIX;
import static org.qubership.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;

@SpringBootTest(properties = {"dbaas.api.opensearch.service.delimiter=-",
        "dbaas.api.opensearch.service.prefix=test",
        "dbaas.api.opensearch.tenant.prefix=tenant-{tenantId}-test",
        "dbaas.api.opensearch.tenant.delimiter=--",
        "dbaas.opensearch.max-conn-total=50",
        "dbaas.opensearch.max-conn-per-route=50"
})
@ContextConfiguration(classes = {OpensearchTestConfiguration.class})
@WebAppConfiguration
@Slf4j
class DbaasOpensearchClientImplTest {

    private static int SECOND = 1000;

    @Autowired
    private OpensearchProperties opensearchProperties;

    @Autowired
    private DbaaSOpensearchConfigurationProperty configurationProperty;

    @Autowired
    @Qualifier(DbaasOpensearchConfiguration.SERVICE_NATIVE_OPENSEARCH_CLIENT)
    private DbaasOpensearchClient serviceClient;

    @Autowired
    @Qualifier(DbaasOpensearchConfiguration.TENANT_NATIVE_OPENSEARCH_CLIENT)
    private DbaasOpensearchClient tenantClient;

    private static String secondIndexName = "second_idx";

    @BeforeEach
    public void clear() throws IOException {
        try {
            serviceClient.getClient().indices().delete(new DeleteIndexRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).build());
        } catch (OpenSearchException e) {
            log.info("Index {} already hasn't exist", TEST_INDEX);
        }
        try {
            serviceClient.getClient().indices().delete(new DeleteIndexRequest.Builder().index(serviceClient.normalize(secondIndexName)).build());
        } catch (OpenSearchException e) {
            log.info("Index {} already hasn't exist", secondIndexName);
        }
    }

    @Test
    void getPrefix() {
        String prefix = serviceClient.getPrefix();
        Assert.assertEquals(TEST_PREFIX, prefix);
    }

    @Test
    void getMaxConnTotal() {
        assertEquals(Integer.valueOf(50), configurationProperty.getMaxConnTotal());
        assertEquals(Integer.valueOf(50), configurationProperty.getMaxConnPerRoute());
    }

    @Test
    void createCustomTenantDatabase() throws IOException { // TODO: probably should add the ability to configure DatabaseConfig in opensearchClient
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("1234"));
        String fullName = tenantClient.normalize("uniq_name");
        Map<String, String> indexData = Map.of("message", "test message");
        IndexRequest<Map<String, String>> indexRequest = new IndexRequest.Builder<Map<String, String>>()
                .index(tenantClient.normalize(TEST_INDEX))
                .id("100")
                .document(indexData)
                .build();
        IndexResponse indexResponse = tenantClient.getClient().index(indexRequest);

        assertEquals("tenant-1234-test", tenantClient.getPrefix());
        assertEquals(tenantClient.getPrefix() + "--uniq_name", fullName);
        assertEquals(tenantClient.getPrefix() + "--" + TEST_INDEX, indexResponse.index());
    }

    @Test
    @DirtiesContext
    void createCustomTenantDatabaseWithCutomBuild() throws IOException { // TODO: probably should add the ability to configure DatabaseConfig in opensearchClient
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("1234"));

        DatabaseConfig.Builder builder = DatabaseConfig.builder();
        DatabaseConfig config = builder.dbNamePrefix("tenant-test1").build();
        OpenSearchClient client = tenantClient.getClient(config);
        IndexRequest<Map<String, String>> indexRequest = new IndexRequest.Builder<Map<String, String>>()
                .index(tenantClient.normalize(config, TEST_INDEX))
                .id("100")
                .document(Map.of("message", "test message"))
                .build();
        IndexResponse indexResponse = client.index(indexRequest);

        Assertions.assertEquals("tenant-test1" + "--" + TEST_INDEX, indexResponse.index());

        client.indices().delete(new DeleteIndexRequest.Builder().index(tenantClient.normalize(TEST_INDEX)).build());
    }

    @Test
    void createCustomDatabase() {
        DatabaseConfig.Builder builder = DatabaseConfig.builder();
        String fullName = serviceClient.normalize("uniq_name");
        Assertions.assertEquals(TEST_PREFIX, serviceClient.getPrefix());
        assertEquals(serviceClient.getPrefix() + "-uniq_name", fullName);
    }

    @Test
    void indices() {
        OpenSearchIndicesClient indices = serviceClient.getClient().indices();
        assertNotNull(indices);
    }

    @Test
    void cluster() {
        OpenSearchClusterClient cluster = serviceClient.getClient().cluster();
        assertNotNull(cluster);
    }

    @Test
    void ingest() {
        OpenSearchIngestClient ingest = serviceClient.getClient().ingest();
        assertNotNull(ingest);
    }

    @Test
    void snapshot() {
        OpenSearchSnapshotClient snapshot = serviceClient.getClient().snapshot();
        assertNotNull(snapshot);
    }

    @Test
    void tasks() {
        OpenSearchTasksClient tasks = serviceClient.getClient().tasks();
        assertNotNull(tasks);
    }

    @Test
    void bulk() throws IOException {
        upsertDocument("1", "get", "get value");
        BulkRequest bulkRequest = new BulkRequest.Builder().operations(
                        new BulkOperation.Builder().index(new IndexOperation.Builder<>().index(serviceClient.normalize(TEST_INDEX)).id("1").document(Map.of("message", "updated test message")).build()).build(),
                        new BulkOperation.Builder().index(new IndexOperation.Builder<>().index(serviceClient.normalize(TEST_INDEX)).id("1").document(Map.of("message", "updated test message")).build()).build())
                .build();
        BulkResponse bulkResponse = serviceClient.getClient().bulk(bulkRequest);
        assertNotNull(bulkResponse);
        String testFullIndex = opensearchProperties.getService().getPrefix() + opensearchProperties.getService().getDelimiter() + TEST_INDEX;
        Assertions.assertEquals("test-" + TEST_INDEX, testFullIndex);
        assertEquals(testFullIndex, bulkResponse.items().get(0).index());
        assertEquals(2, bulkResponse.items().get(0).version());
        assertEquals(testFullIndex, bulkResponse.items().get(1).index());
        assertEquals(3, bulkResponse.items().get(1).version());
    }

    @Test
    void reindex() throws IOException, InterruptedException {
        String destinationIndex = "dest_reindex";
        upsertDocument("1", "get", "get value");
        Thread.sleep(SECOND);
        ReindexRequest reindexRequest = new ReindexRequest.Builder()
                .source(new Source.Builder().index(serviceClient.normalize(TEST_INDEX)).build())
                .dest(new Destination.Builder().index(serviceClient.normalize(destinationIndex)).build())
                .build();
        ReindexResponse reindexResponse = serviceClient.getClient().reindex(reindexRequest);
        assertNotNull(reindexResponse);
        Thread.sleep(5L * SECOND);
        GetRequest getRequest = new GetRequest.Builder().index(serviceClient.normalize(destinationIndex)).id("1").build();
        GetResponse getResponse = serviceClient.getClient().get(getRequest, Map.class);
        assertTrue(getResponse.found());

        deleteIndex(destinationIndex);
    }

    @Test
    void updateByQueryOneIndex() throws IOException, InterruptedException {
        upsertDocument("1", "User", "Kimchy");
        Thread.sleep(SECOND);
        UpdateByQueryRequest request = new UpdateByQueryRequest.Builder().index(serviceClient.normalize(TEST_INDEX))
                .query(new Query.Builder().matchAll(new MatchAllQuery.Builder().build()).build())
                .script(new Script.Builder().inline(new InlineScript.Builder().source("ctx._source.Field=3;").build()).build())
                .build();
        UpdateByQueryResponse bulkResponse = serviceClient.getClient().updateByQuery(request);
        assertNotNull(bulkResponse);
        assertEquals(1, bulkResponse.updated());

        GetRequest getRequest = new GetRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).id("1").build();
        GetResponse<Map> getResponse = serviceClient.getClient().get(getRequest, Map.class);
        assertTrue(getResponse.found());
        assertEquals(3, getResponse.source().get("Field"));
    }

    @Test
    void updateByQueryManyIndex() throws IOException, InterruptedException {
        upsertDocument("1", "one", "first doc");
        upsertDocument("1", "one", "another docs", secondIndexName);
        Thread.sleep(SECOND);
        UpdateByQueryRequest request = new UpdateByQueryRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX), serviceClient.normalize(secondIndexName))
                .query(new Query.Builder().matchAll(new MatchAllQuery.Builder().build()).build())
                .script(new Script.Builder().inline(new InlineScript.Builder().source("ctx._source.Field=3;").build()).build())
                .build();
        UpdateByQueryResponse bulkResponse = serviceClient.getClient().updateByQuery(request);
        assertNotNull(bulkResponse);
        assertEquals(2, bulkResponse.updated());

        GetRequest getRequest = new GetRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).id("1").build();
        GetResponse<Map> getResponse = serviceClient.getClient().get(getRequest, Map.class);
        assertTrue(getResponse.found());
        assertEquals(3, getResponse.source().get("Field"));

        GetRequest secondGetRequest = new GetRequest.Builder().index(serviceClient.normalize(secondIndexName)).id("1").build();
        GetResponse<Map> getResponseFromSecond = serviceClient.getClient().get(secondGetRequest, Map.class);
        assertTrue(getResponseFromSecond.found());
        assertEquals(3, getResponseFromSecond.source().get("Field"));
    }

    @Test
    void deleteByQueryFromManyIndex() throws IOException, InterruptedException {
        upsertDocument("1", "User", "Kimchy");
        upsertDocument("1", "User", "Twitter", secondIndexName);
        Thread.sleep(SECOND);

        DeleteByQueryRequest request = new DeleteByQueryRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX), serviceClient.normalize(secondIndexName))
                .query(new Query.Builder().matchAll(new MatchAllQuery.Builder().build()).build())
                .build();
        DeleteByQueryResponse bulkResponse = serviceClient.getClient().deleteByQuery(request);
        assertNotNull(bulkResponse);
        assertEquals(2, bulkResponse.deleted());

        GetRequest getRequest = new GetRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).id("1").build();
        GetResponse<Map> getResponse = serviceClient.getClient().get(getRequest, Map.class);
        assertFalse(getResponse.found());

        GetRequest secondGetRequest = new GetRequest.Builder().index(serviceClient.normalize(secondIndexName)).id("1").build();
        GetResponse<Map> getResponseFromSecond = serviceClient.getClient().get(secondGetRequest, Map.class);
        assertFalse(getResponseFromSecond.found());
    }

    @Test
    void ping() throws IOException {
        boolean ping = serviceClient.getClient().ping().value();
        assertTrue(ping);
    }

    @Test
    void get() throws IOException {
        upsertDocument("1", "get", "get value");
        GetRequest getRequest = new GetRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).id("1").build();
        GetResponse<Map> getResponse = serviceClient.getClient().get(getRequest, Map.class);
        assertNotNull(getResponse);
        assertTrue(getResponse.found());
        assertEquals("1", getResponse.id());
        Map<String, Object> responseBody = getResponse.source();
        assertEquals(1, responseBody.size());
        assertEquals("get value", responseBody.get("get"));
    }

    @Test
    void getNotFound() throws IOException {
        upsertDocument("2", "Key", "Value");
        GetRequest getRequest = new GetRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).id("1").build();
        GetResponse<Map> getResponse = serviceClient.getClient().get(getRequest, Map.class);
        assertNotNull(getResponse);
        assertFalse(getResponse.found());
    }

    @Test
    void mget() throws IOException {
        upsertDocument("example_id", "Test-key", "test-Val");
        upsertDocument("another_id", "Second-key", "Second-Val", secondIndexName);
        MgetRequest request = new MgetRequest.Builder()
                .docs(
                        new MultiGetOperation.Builder().index(serviceClient.normalize(TEST_INDEX)).id("example_id").build(),
                        new MultiGetOperation.Builder().index(serviceClient.normalize(secondIndexName)).id("another_id").build()
                )
                .build();
        MgetResponse<Map> response = serviceClient.getClient().mget(request, Map.class);
        assertNotNull(response);

        List<MultiGetResponseItem<Map>> responses = response.docs();
        assertEquals(2, responses.size());

        GetResult<Map> testResponse = responses.get(0).result();
        assertTrue(testResponse.found());
        assertEquals("example_id", testResponse.id());

        GetResult<Map> secondResponse = responses.get(1).result();
        assertTrue(secondResponse.found());
        assertEquals("another_id", secondResponse.id());
    }

    @Test
    void exists() throws IOException {
        upsertDocument("1", "key", "value");
        ExistsRequest getRequest = new ExistsRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .id("1")
                .build();
        boolean exists = serviceClient.getClient().exists(getRequest).value();
        assertTrue(exists);
    }

    @Test
    void existsSource() throws IOException {
        upsertDocument("1", "Key", "Value");
        ExistsSourceRequest existsSourceRequest = new ExistsSourceRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).id("1").build();
        boolean existsSource = serviceClient.getClient().existsSource(existsSourceRequest).value();
        assertTrue(existsSource);
    }

    @Test
    void getSource() throws IOException {
        upsertDocument("1", "Key", "Value");
        GetSourceRequest getSourceRequest = new GetSourceRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).id("1").build();
        GetSourceResponse<Map> response = serviceClient.getClient().getSource(getSourceRequest, Map.class);
        assertNotNull(response);
        Map<String, Object> source = response.valueBody();
        assertTrue(source.containsKey("Key"));
        assertEquals("Value", source.get("Key"));
    }

    @Test
    void index() throws IOException {
        IndexRequest<Map<String, String>> indexRequest = new IndexRequest.Builder<Map<String, String>>()
                .index(serviceClient.normalize(TEST_INDEX))
                .id("100")
                .document(Map.of("message", "test message"))
                .build();
        IndexResponse indexResponse = serviceClient.getClient().index(indexRequest);
        assertNotNull(indexResponse);
        assertEquals(Result.Created, indexResponse.result());

        IndexRequest<Map<String, String>> updateIndexRequest = new IndexRequest.Builder<Map<String, String>>()
                .index(serviceClient.normalize(TEST_INDEX))
                .id("100")
                .document(Map.of("message", "updated test message"))
                .build();
        IndexResponse updatedIndexResponse = serviceClient.getClient().index(updateIndexRequest);
        assertNotNull(updatedIndexResponse);
        assertEquals(Result.Updated, updatedIndexResponse.result());
    }

    @Test
    void count() throws IOException, InterruptedException {
        upsertDocument("1", "count", "count value");
        upsertDocument("2", "count", "another count value", secondIndexName);
        Thread.sleep(SECOND);
        CountRequest countRequest = new CountRequest.Builder().index(serviceClient.normalize(TEST_INDEX), serviceClient.normalize(secondIndexName)).build();
        CountResponse countResponse = serviceClient.getClient().count(countRequest);
        assertEquals(2, countResponse.count());
    }

    @Test
    void update() throws IOException, InterruptedException {
        upsertDocument("1", "update", "update value");
        Thread.sleep(SECOND);
        GetRequest getRequestBeforeUpdate = new GetRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).id("1").build();
        GetResponse<Map> firstGetResponse = serviceClient.getClient().get(getRequestBeforeUpdate, Map.class);
        assertEquals(1, firstGetResponse.version());
        UpdateRequest<Map, Map> request = new UpdateRequest.Builder<Map, Map>()
                .index(serviceClient.normalize(TEST_INDEX))
                .id("1")
                .doc(Map.of("updated", "updated value"))
                .build();
        UpdateResponse<Map> updateResponse = serviceClient.getClient().update(request, Map.class);
        assertEquals(Result.Updated, updateResponse.result());
        GetRequest getRequestAfterUpdate = new GetRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).id("1").build();
        GetResponse<Map> getResponseAfterUpdate = serviceClient.getClient().get(getRequestAfterUpdate, Map.class);
        assertEquals(2, getResponseAfterUpdate.version());
        assertEquals("updated value", getResponseAfterUpdate.source().get("updated"));
    }

    @Test
    void delete() throws IOException {
        upsertDocument("1", "delete", "value for deletion");
        DeleteRequest request = new DeleteRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).id("1").build();
        DeleteResponse deleteResponse = serviceClient.getClient().delete(request);
        assertEquals(Result.Deleted, deleteResponse.result());
        GetRequest getRequest = new GetRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).id("1").build();
        GetResponse<Map> getResponse = serviceClient.getClient().get(getRequest, Map.class);
        assertFalse(getResponse.found());
    }

    @Test
    void deleteNotFound() throws IOException {
        upsertDocument("2", "Key", "Value");
        DeleteRequest request = new DeleteRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).id("1").build();
        DeleteResponse deleteResponse = serviceClient.getClient().delete(request);
        assertEquals(Result.NotFound, deleteResponse.result());
    }

    @Test
    void search() throws IOException, InterruptedException {
        upsertDocument("1", "search", "value-search");
        Thread.sleep(SECOND);
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .query(new Query.Builder().matchAll(new MatchAllQuery.Builder().build()).build())
                .build();
        SearchResponse<Map> searchResponse = serviceClient.getClient().search(searchRequest, Map.class);

        assertNotNull(searchResponse);
        HitsMetadata<Map> hits = searchResponse.hits();
        assertEquals(1, hits.hits().size());
        Hit<Map> hit = hits.hits().get(0);
        Map<String, Object> sourceAsMap = hit.source();
        assertEquals("value-search", sourceAsMap.get("search"));
    }

    @Test
    void msearch() throws IOException, InterruptedException {
        upsertDocument("1", "FirstKey", "FirstValue");
        upsertDocument("1", "SecondKey", "SecondValue", secondIndexName);
        Thread.sleep(SECOND);

        MsearchRequest request = new MsearchRequest.Builder()
                .searches(
                        new RequestItem.Builder()
                                .header(new MultisearchHeader.Builder().index(serviceClient.normalize(TEST_INDEX)).build())
                                .body(new MultisearchBody.Builder().query(new Query.Builder().match(new MatchQuery.Builder()
                                        .field("FirstKey")
                                        .query(new FieldValue.Builder().stringValue("FirstValue").build())
                                        .build()).build()).build())
                                .build(),
                        new RequestItem.Builder()
                                .header(new MultisearchHeader.Builder().index(serviceClient.normalize(secondIndexName)).build())
                                .body(new MultisearchBody.Builder().query(new Query.Builder().match(new MatchQuery.Builder()
                                        .field("SecondKey")
                                        .query(new FieldValue.Builder().stringValue("SecondValue").build())
                                        .build()).build()).build())
                                .build()
                ).build();

        MsearchResponse<Map> response = serviceClient.getClient().msearch(request, Map.class);
        assertNotNull(response);
        List<MultiSearchResponseItem<Map>> responses = response.responses();
        assertEquals(2, responses.size());

        MultiSearchItem<Map> firstResponse = responses.get(0).result();
        Hit<Map> hit = firstResponse.hits().hits().get(0);
        Map<String, Object> sourceAsMap = hit.source();
        assertEquals("FirstValue", sourceAsMap.get("FirstKey"));

        MultiSearchItem<Map> secondResponse = responses.get(1).result();
        Hit<Map> secondHit = secondResponse.hits().hits().get(0);
        Map<String, Object> secondSourceAsMap = secondHit.source();
        assertEquals("SecondValue", secondSourceAsMap.get("SecondKey"));
    }

    @Test
    void scroll() throws IOException {
        upsertDocument("1", "Key", "Value");
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .query(new Query.Builder().matchAll(new MatchAllQuery.Builder().build()).build())
                .scroll(new Time.Builder().time("30s").build())
                .build();
        SearchResponse<Map> searchResponse = serviceClient.getClient().search(searchRequest, Map.class);
        String scrollId = searchResponse.scrollId();
        assertNotNull(scrollId);

        ScrollRequest scrollRequest = new ScrollRequest.Builder()
                .scrollId(scrollId)
                .scroll(new Time.Builder().time("30s").build())
                .build();
        SearchResponse<Map> searchScrollResponse = serviceClient.getClient().scroll(scrollRequest, Map.class);
        scrollId = searchScrollResponse.scrollId();
        assertNotNull(scrollId);
    }

    @Test
    void clearScroll() throws IOException {
        upsertDocument("1", "Key", "Value");
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .query(new Query.Builder().matchAll(new MatchAllQuery.Builder().build()).build())
                .scroll(new Time.Builder().time("5m").build())
                .build();
        SearchResponse<Map> searchResponse = serviceClient.getClient().search(searchRequest, Map.class);
        String scrollId = searchResponse.scrollId();
        assertNotNull(scrollId);

        ClearScrollRequest request = new ClearScrollRequest.Builder().scrollId(scrollId).build();
        ClearScrollResponse response = serviceClient.getClient().clearScroll(request);
        assertTrue(response.succeeded());
    }

    @Test
    void searchTemplate() throws IOException, InterruptedException {
        upsertDocument("1", "Key", "Value");
        upsertDocument("2", "Key", "Value", secondIndexName);
        Thread.sleep(SECOND);

        Map<String, JsonData> scriptParams = new HashMap<>();
        scriptParams.put("field", JsonData.of("Key"));
        scriptParams.put("value", JsonData.of("Value"));
        scriptParams.put("size", JsonData.of(2));

        SearchTemplateRequest request = new SearchTemplateRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX), serviceClient.normalize(secondIndexName))
                .id("posts")
                .source("{" +
                        "  \"query\": { \"match\" : { \"{{field}}\" : \"{{value}}\" } }," +
                        "  \"size\" : \"{{size}}\"" +
                        "}")
                .params(scriptParams)
                .build();

        SearchTemplateResponse<Map> response = serviceClient.getClient().searchTemplate(request, Map.class);
        List<Hit<Map>> hits = response.hits().hits();
        assertEquals(2, hits.size());
    }

    @Test
    void explain() throws IOException, InterruptedException {
        upsertDocument("1", "Key", "Value");
        Thread.sleep(SECOND);
        ExplainRequest request = new ExplainRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .id("1")
                .query(new Query.Builder().matchAll(new MatchAllQuery.Builder().build()).build())
                .build();
        ExplainResponse<Map> response = serviceClient.getClient().explain(request, Map.class);
        assertEquals("1", response.id());
        assertTrue(response.matched());
        assertNotNull(response.explanation());
    }

    @Test
    void termvectors() throws IOException, InterruptedException {
        upsertDocument("1", "Key", "Value");
        Thread.sleep(SECOND);
        TermvectorsRequest<Map> request = new TermvectorsRequest.Builder<Map>()
                .index(serviceClient.normalize(TEST_INDEX))
                .id("1")
                .fields("Key")
                .build();
        TermvectorsResponse response = serviceClient.getClient().termvectors(request);
        assertTrue(response.found());
    }

    @Test
    void rankEval() throws IOException, InterruptedException {
        upsertDocument("1", "user", "kimchy");
        Thread.sleep(SECOND);
        RankEvalRequest request = new RankEvalRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .metric(new RankEvalMetric.Builder()
                        .precision(new RankEvalMetricPrecision.Builder()
                                .k(1)
                                .build())
                        .build())
                .requests(new RankEvalRequestItem.Builder()
                        .id("kimchy_query")
                        .request(new RankEvalQuery.Builder()
                                .query(new Query.Builder()
                                        .match(new MatchQuery.Builder()
                                                .field("user")
                                                .query(new FieldValue.Builder().stringValue("kimchy").build())
                                                .build())
                                        .build())
                                .build())
                        .ratings(new DocumentRating.Builder()
                                .index(serviceClient.normalize(TEST_INDEX))
                                .id("1")
                                .rating(1)
                                .build())
                        .build())
                .build();
        RankEvalResponse response = serviceClient.getClient().rankEval(request);
        Map<String, RankEvalMetricDetail> partialResults = response.details();
        RankEvalMetricDetail evalQuality = partialResults.get("kimchy_query");
        assertNotNull(evalQuality);
    }

    @Test
    void msearchTemplate() throws IOException {
        upsertDocument("1", "title", "val1");
        upsertDocument("2", "title", "val2");
        upsertDocument("3", "title", "val3");
        String[] searchTerms = {"val1", "val2", "val3"};

        List<org.opensearch.client.opensearch.core.msearch_template.RequestItem> requestItems = new ArrayList<>();
        for (String searchTerm : searchTerms) {
            Map<String, JsonData> scriptParams = new HashMap<>();
            scriptParams.put("field", JsonData.of("title"));
            scriptParams.put("value", JsonData.of(searchTerm));
            scriptParams.put("size", JsonData.of(5));

            requestItems.add(new org.opensearch.client.opensearch.core.msearch_template.RequestItem.Builder()
                    .header(new MultisearchHeader.Builder().index(serviceClient.normalize(TEST_INDEX)).build())
                    .body(new TemplateConfig.Builder()
                            .id("posts")
                            .source("{" +
                                    "  \"query\": { \"match\" : { \"{{field}}\" : \"{{value}}\" } }," +
                                    "  \"size\" : \"{{size}}\"" +
                                    "}")
                            .params(scriptParams)
                            .build())
                    .build());
        }

        MsearchTemplateRequest multiRequest = new MsearchTemplateRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX))
                .searchTemplates(requestItems)
                .build();
        MsearchTemplateResponse<Map> multiResponse = serviceClient.getClient().msearchTemplate(multiRequest, Map.class);
        assertEquals(3, multiResponse.responses().size());
    }

    @Test
    void fieldCaps() throws IOException, InterruptedException {
        upsertDocument("1", "Key", "Val");
        upsertDocument("1", "Key", "Val", secondIndexName);
        Thread.sleep(SECOND);
        FieldCapsRequest request = new FieldCapsRequest.Builder()
                .index(serviceClient.normalize(TEST_INDEX), serviceClient.normalize(secondIndexName))
                .fields("Key")
                .build();
        FieldCapsResponse response = serviceClient.getClient().fieldCaps(request);
        Map<String, FieldCapability> userResponse = response.fields().get("Key");
        FieldCapability textCapabilities = userResponse.get("text");

        assertNotNull(textCapabilities);
        assertTrue(textCapabilities.searchable());
        assertFalse(textCapabilities.aggregatable());
    }

    private void upsertDocument(String id, String key, String value) throws IOException {
        upsertDocument(id, key, value, TEST_INDEX);
    }

    private void upsertDocument(String id, String key, String value, String indexName) throws IOException {
        IndexRequest<Map<String, String>> updateIndexRequest = new IndexRequest.Builder<Map<String, String>>()
                .index(serviceClient.normalize(indexName))
                .id(id)
                .document(Map.of(key, value))
                .build();
        serviceClient.getClient().index(updateIndexRequest);
    }

    private void deleteIndex(String indexName) throws IOException {
        DeleteIndexRequest request = new DeleteIndexRequest.Builder().index(serviceClient.normalize(indexName)).build();
        AcknowledgedResponseBase deleteIndexResponse = serviceClient.getClient().indices().delete(request);
        assertTrue(deleteIndexResponse.acknowledged());
    }
}
