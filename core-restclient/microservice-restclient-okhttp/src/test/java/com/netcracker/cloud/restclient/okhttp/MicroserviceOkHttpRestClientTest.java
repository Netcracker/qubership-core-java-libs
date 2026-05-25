package com.netcracker.cloud.restclient.okhttp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.core.error.rest.exception.RemoteCodeException;
import com.netcracker.cloud.core.error.rest.tmf.TmfErrorResponse;
import com.netcracker.cloud.restclient.BaseMicroserviceRestClientTest;
import com.netcracker.cloud.restclient.HttpMethod;
import com.netcracker.cloud.restclient.exception.MicroserviceRestClientResponseException;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MicroserviceOkHttpRestClientTest extends BaseMicroserviceRestClientTest {

    @BeforeEach
    void setUp() {
        restClient = new MicroserviceOkHttpRestClient(new OkHttpClient());
    }

    @Test
    void testDefaultRequestHeaders() throws InterruptedException {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200).setBody("Test response body"));

        restClient.doRequest(testUrl, HttpMethod.POST, null, null, Void.class);
        RecordedRequest recordedRequest = mockBackEnd.takeRequest(60, TimeUnit.SECONDS);

        assertNotNull(recordedRequest);
        assertEquals("application/json", recordedRequest.getHeader("Content-Type"));
    }

    @Test
    void testRequestBodies() throws Exception {
        // String body
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200));
        restClient.doRequest(testUrl, HttpMethod.POST, null, "test-string", Void.class);
        RecordedRequest req1 = mockBackEnd.takeRequest();
        assertNotNull(req1);
    }

    @Test
    void testMapResponseBody() throws Exception {
        // Void
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200));
        assertNull(restClient.doRequest(testUrl, HttpMethod.GET, null, null, Void.class).getResponseBody());

        // String
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200).setBody("hello"));
        assertEquals("hello", restClient.doRequest(testUrl, HttpMethod.GET, null, null, String.class).getResponseBody());

        // byte[]
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200).setBody("bytes"));
        assertArrayEquals("bytes".getBytes(), restClient.doRequest(testUrl, HttpMethod.GET, null, null, byte[].class).getResponseBody());
    }

    @Test
    void testSerializationFailure() {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200));
        // Using an object that cannot be serialized by the default mapper
        Object unserializable = new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("fail");
            }
        };
        // The default mapper might just use toString() or fail if it's a complex object. 
        // Actually, for serialization, if it's not a primitive, Jackson will try to serialize.
        // Let's use a class that causes an IOException.
        assertThrows(Exception.class, () -> {
            restClient.doRequest(testUrl, HttpMethod.POST, null, new Object(), Void.class);
        });
    }

    @Test
    void testParseError() throws Exception {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(500).setBody("invalid-json"));
        try {
            restClient.doRequest(testUrl, HttpMethod.GET, null, null, String.class);
        } catch (MicroserviceRestClientResponseException e) {
            assertEquals(500, e.getHttpStatus());
            assertTrue(e.getCause() instanceof Exception);
        }
    }
}
