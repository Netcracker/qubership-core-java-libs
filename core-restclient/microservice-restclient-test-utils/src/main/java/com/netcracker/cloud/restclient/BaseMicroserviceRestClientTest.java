package com.netcracker.cloud.restclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Headers;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.restclient.entity.RestClientResponseEntity;
import org.qubership.cloud.restclient.entity.TestEntity;
import org.qubership.cloud.restclient.exception.MicroserviceRestClientResponseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * NOTE: there is a QuarkusMicroserviceRestClientTest based on this. It must be executed as well  in case of modification
 * of this class.
 *
 * <p>
 * It does not have a default (de)serializer for {@link TestEntity} thus you have to pass only json
 * as body and read {@link String} from response.
 * <p>
 * Also {@code Content-type} header is mandatory for request/response in case if you want to
 * pass/get body
 */
public abstract class BaseMicroserviceRestClientTest {
    protected static final String TEST_PATH = "/test-path?param1=val1";
    protected static final String APPLICATION_JSON = "application/json";
    private static final String ANOTHER_TEST_HEADER_NAME = "Another-Test-Header-Name";

    private static final String LOCAL_HOST = "http://localhost:";

    protected static final String TEST_RESPONSE_BODY = "Test response body";

    protected static final String CONTENT_TYPE = "Content-Type";
    private static final String TEST_HEADER_NAME = "Test-Header-Name";
    protected static MockWebServer mockBackEnd;
    protected static String testUrl;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected MicroserviceRestClient restClient;

    @BeforeAll
    static void setUpClassBase() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        testUrl = LOCAL_HOST + mockBackEnd.getPort() + TEST_PATH;
    }

    @AfterAll
    static void tearDownClassBase() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void testNullRequestBody() throws InterruptedException {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_RESPONSE_BODY));

        RestClientResponseEntity<Void> response = restClient.doRequest(URI.create(testUrl), HttpMethod.POST, null, null, Void.class);
        assertEquals(200, response.getHttpStatus());

        mockBackEnd.takeRequest(60, TimeUnit.SECONDS); // take request to not affect other tests
    }

    @Test
    void testNonNullRequestBody() throws InterruptedException {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_RESPONSE_BODY));

        final String testReqBody = "{ \"id\": 1, \"name\": \"some-test-object\" }";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, Collections.singletonList("text/plain"));
        RestClientResponseEntity<Void> response = restClient.doRequest(URI.create(testUrl), HttpMethod.POST, headers, testReqBody, Void.class);
        assertEquals(200, response.getHttpStatus());

        RecordedRequest request = mockBackEnd.takeRequest(60, TimeUnit.SECONDS); // take request to not affect other tests
        assertEquals(testReqBody, request.getBody().readString(UTF_8));
    }

    @Test
    void testUrlWithParams() throws InterruptedException {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_RESPONSE_BODY));

        RestClientResponseEntity<Void> response = restClient.doRequest(testUrl, HttpMethod.POST, null, null, Void.class);
        RecordedRequest recordedRequest = mockBackEnd.takeRequest(60, TimeUnit.SECONDS);
        assertEquals(200, response.getHttpStatus());
        assertNotNull(recordedRequest);
        assertEquals(TEST_PATH, recordedRequest.getPath());
    }

    @Test
    void testUriTemplate() throws InterruptedException {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_RESPONSE_BODY));

        Map<String, Object> uriParams = new HashMap<>(1);
        uriParams.put("param1", "val1");
        RestClientResponseEntity<Void> response = restClient.doRequest(LOCAL_HOST + mockBackEnd.getPort() + "/test-path?param1={param1}", HttpMethod.POST, null, null, Void.class, uriParams);
        RecordedRequest recordedRequest = mockBackEnd.takeRequest(60, TimeUnit.SECONDS);
        assertEquals(200, response.getHttpStatus());
        assertNotNull(recordedRequest);
        assertEquals(TEST_PATH, recordedRequest.getPath());
    }

    @Test
    void testRequestHeaders() throws InterruptedException {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_RESPONSE_BODY));
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_RESPONSE_BODY));

        RestClientResponseEntity<Void> response = restClient.doRequest(testUrl, HttpMethod.POST, null, null, Void.class);
        RecordedRequest recordedRequest = mockBackEnd.takeRequest(60, TimeUnit.SECONDS);
        assertEquals(200, response.getHttpStatus());
        assertNotNull(recordedRequest);
        assertNull(recordedRequest.getHeader(TEST_HEADER_NAME));

        Map<String, List<String>> httpHeaders = new HashMap<>();
        httpHeaders.put(ANOTHER_TEST_HEADER_NAME, Collections.singletonList("Another-Header-Value"));

        response = restClient.doRequest(testUrl, HttpMethod.POST, httpHeaders, null, Void.class);
        recordedRequest = mockBackEnd.takeRequest(60, TimeUnit.SECONDS);
        assertEquals(200, response.getHttpStatus());
        assertNotNull(recordedRequest);
        assertNull(recordedRequest.getHeader(TEST_HEADER_NAME));
        assertEquals("Another-Header-Value", recordedRequest.getHeader(ANOTHER_TEST_HEADER_NAME));
    }

    @Test
    void testResponseHeaders() throws InterruptedException {
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(TEST_HEADER_NAME, "Test-Header-Response-Value")
                .setHeader(ANOTHER_TEST_HEADER_NAME, "Another-Test-Header-Response-Value")
                .setBody(TEST_RESPONSE_BODY));

        Map<String, List<String>> httpHeaders = new HashMap<>();
        httpHeaders.put(TEST_HEADER_NAME, Collections.singletonList("Test-Header-Value"));

        RestClientResponseEntity<Void> response = restClient.doRequest(testUrl, HttpMethod.POST, httpHeaders, null, Void.class);
        RecordedRequest recordedRequest = mockBackEnd.takeRequest(60, TimeUnit.SECONDS);

        assertNotNull(recordedRequest);
        assertEquals(200, response.getHttpStatus());
        assertEquals("Test-Header-Value", recordedRequest.getHeader(TEST_HEADER_NAME));
        assertEquals("Test-Header-Response-Value", response.getHeaders().get(TEST_HEADER_NAME).get(0));
        assertEquals("Another-Test-Header-Response-Value", response.getHeaders().get(ANOTHER_TEST_HEADER_NAME).get(0));
    }

    @Test
    void testResponseBodyMapping() throws InterruptedException, IOException {
        final String singleEntity = objectMapper.writeValueAsString(TestEntity.createTestEntity());
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setBody(singleEntity));

        RestClientResponseEntity<String> response = restClient.doRequest(testUrl, HttpMethod.POST, null, null, String.class);
        RecordedRequest recordedRequest = mockBackEnd.takeRequest(60, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals(200, response.getHttpStatus());
        assertEquals(singleEntity, response.getResponseBody());

        final List<TestEntity> collectionOfEntities = new ArrayList<>(3);
        collectionOfEntities.add(TestEntity.createTestEntity());
        collectionOfEntities.add(TestEntity.createTestEntity());
        collectionOfEntities.add(TestEntity.createTestEntity());
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setBody(objectMapper.writeValueAsString(collectionOfEntities)));

        RestClientResponseEntity<String> secondResponse = restClient.doRequest(testUrl, HttpMethod.POST, null,
                null, String.class);
        recordedRequest = mockBackEnd.takeRequest(60, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals(200, response.getHttpStatus());
        assertEquals(collectionOfEntities, Arrays.asList(objectMapper.readValue(secondResponse.getResponseBody(), TestEntity[].class)));
    }

    @Test
    void testResponseException() throws InterruptedException {
        final String errBody = "Test internal server error";
        final Map<String, List<String>> errResponseHeaders = new HashMap<>();
        errResponseHeaders.put("test-err-header", Collections.singletonList("test-err-header-value"));
        errResponseHeaders.put("another-test-err-header", Collections.singletonList("another-test-err-header-value"));
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeaders(convertHeaders(errResponseHeaders))
                .setHeader(CONTENT_TYPE, "text/plain")
                .setBody(errBody));

        boolean gotExpectedException = false;
        try {
            restClient.doRequest(testUrl, HttpMethod.POST, null, null, Void.class);
        } catch (MicroserviceRestClientResponseException e) {
            gotExpectedException = true;
            assertEquals(500, e.getHttpStatus());
            assertEquals(errBody, e.getResponseBodyAsString());
            assertTrue(e.getResponseHeaders().entrySet().containsAll(errResponseHeaders.entrySet()));
        }
        mockBackEnd.takeRequest(60, TimeUnit.SECONDS);
        assertTrue(gotExpectedException);
    }

    private Headers convertHeaders(Map<String, List<String>> errResponseHeaders) {
        return Headers.of(errResponseHeaders.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)))
        );
    }

    @Test
    void testResponseExceptionUnknownStatusCode() throws InterruptedException {
        final String errBody = "Test internal server error";
        final Map<String, List<String>> errResponseHeaders = new HashMap<>();
        errResponseHeaders.put("test-err-header", Collections.singletonList("test-err-header-value"));
        errResponseHeaders.put("another-test-err-header", Collections.singletonList("another-test-err-header-value"));
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(999)
                .setHeaders(convertHeaders(errResponseHeaders))
                .setBody(errBody));

        boolean gotExpectedException = false;
        try {
            restClient.doRequest(testUrl, HttpMethod.POST, null, null, String.class);
        } catch (MicroserviceRestClientResponseException e) {
            gotExpectedException = true;
            assertTrue(e.getHttpStatus() > 0);
            assertTrue(e.getResponseHeaders().entrySet().containsAll(errResponseHeaders.entrySet()));
        }
        mockBackEnd.takeRequest(60, TimeUnit.SECONDS);
        assertTrue(gotExpectedException);
    }

    @Test
    void testOverloads() throws InterruptedException, IOException {
        final String respBody = TEST_RESPONSE_BODY;
        final Map<String, List<String>> responseHeaders = new HashMap<>();
        responseHeaders.put("test-header", Collections.singletonList("test-header-value"));
        responseHeaders.put("another-test-header", Collections.singletonList("another-test-header-value"));
        // enqueue 3 responses for testing 3 doRequest() method overloads
        for (int i = 0; i < 3; i++) {
            mockBackEnd.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeaders(convertHeaders(responseHeaders))
                    .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .setBody(respBody));
        }

        final String reqBody = objectMapper.writeValueAsString(TestEntity.createTestEntity());
        final Map<String, List<String>> requestHeaders = new HashMap<>();
        requestHeaders.put("test-header", Collections.singletonList("test-req-header-value"));
        requestHeaders.put("another-test-header", Collections.singletonList("another-test-req-header-value"));
        requestHeaders.put(CONTENT_TYPE, Collections.singletonList(APPLICATION_JSON));

        RestClientResponseEntity<String> firstResponse = restClient.doRequest(URI.create(testUrl), HttpMethod.POST, requestHeaders, reqBody, String.class);
        RecordedRequest firstRequest = mockBackEnd.takeRequest(60, TimeUnit.SECONDS);

        Map<String, Object> uriParams = new HashMap<>(1);
        uriParams.put("param1", "val1");
        RestClientResponseEntity<String> secondResponse = restClient.doRequest(
                LOCAL_HOST + mockBackEnd.getPort() + "/test-path?param1={param1}", HttpMethod.POST,
                requestHeaders, reqBody, String.class, uriParams);
        RecordedRequest secondRequest = mockBackEnd.takeRequest(60, TimeUnit.SECONDS);

        RestClientResponseEntity<String> thirdResponse = restClient.doRequest(URI.create(testUrl), HttpMethod.POST, requestHeaders, reqBody, String.class);
        RecordedRequest thirdRequest = mockBackEnd.takeRequest(60, TimeUnit.SECONDS);

        assertEquals(TEST_PATH, firstRequest.getPath());
        assertTrue(firstRequest.getHeaders().toMultimap().entrySet().containsAll(requestHeaders.entrySet()));
        assertEquals(reqBody, new BufferedReader(new InputStreamReader(firstRequest.getBody().inputStream())).lines().collect(Collectors.joining("\n")));

        assertEquals(TEST_PATH, secondRequest.getPath());
        assertTrue(secondRequest.getHeaders().toMultimap().entrySet().containsAll(requestHeaders.entrySet()));
        assertEquals(reqBody, new BufferedReader(new InputStreamReader(secondRequest.getBody().inputStream())).lines().collect(Collectors.joining("\n")));

        assertEquals(TEST_PATH, thirdRequest.getPath());
        assertTrue(thirdRequest.getHeaders().toMultimap().entrySet().containsAll(requestHeaders.entrySet()));
        assertEquals(reqBody, new BufferedReader(new InputStreamReader(thirdRequest.getBody().inputStream())).lines().collect(Collectors.joining("\n")));

        assertEquals(200, firstResponse.getHttpStatus());
        assertTrue(firstResponse.getHeaders().entrySet().containsAll(responseHeaders.entrySet()));
        assertEquals(respBody, firstResponse.getResponseBody());

        assertEquals(firstResponse, secondResponse);
        assertEquals(firstResponse, thirdResponse);
    }
}
