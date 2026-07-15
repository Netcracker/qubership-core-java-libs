package com.netcracker.cloud.restclient.okhttp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.core.error.rest.exception.RemoteCodeException;
import com.netcracker.cloud.core.error.rest.tmf.TmfErrorResponse;
import com.netcracker.cloud.framework.contexts.tenant.TenantContextObject;
import com.netcracker.cloud.framework.contexts.tenant.context.TenantContext;
import com.netcracker.cloud.restclient.BaseMicroserviceRestClientTest;
import com.netcracker.cloud.restclient.HttpMethod;
import com.netcracker.cloud.restclient.exception.MicroserviceRestClientResponseException;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MicroserviceOkHttpRestClientTest extends BaseMicroserviceRestClientTest {

    @BeforeEach
    void setUp() {
        restClient = new MicroserviceOkHttpRestClient(new OkHttpClient());
    }

    @AfterEach
    void tearDown() {
        ContextManager.clearAll();
    }

    @Test
    void testTenantContextPropagation() throws InterruptedException {
        TenantContext.set("test-tenant");
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200));

        restClient.doRequest(testUrl, HttpMethod.GET, null, null, Void.class);
        RecordedRequest recordedRequest = mockBackEnd.takeRequest(60, TimeUnit.SECONDS);

        assertNotNull(recordedRequest);
        assertEquals("test-tenant", recordedRequest.getHeader(TenantContextObject.TENANT_HEADER));
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
    void testTMFRestClientResponseException() throws Exception {
        TmfErrorResponse tmfErrorResponse = TmfErrorResponse.builder()
                .id(UUID.randomUUID().toString())
                .code("TEST")
                .reason("test reason")
                .detail("test detail")
                .status("500")
                .type(TmfErrorResponse.TYPE_V1_0)
                .build();

        mockBackEnd.enqueue(new MockResponse()
                .setHeader("test-header", "test-value")
                .setResponseCode(500)
                .setBody(new ObjectMapper().writeValueAsString(tmfErrorResponse)));

        try {
            restClient.doRequest(testUrl, HttpMethod.POST, null, null, Void.class);
            fail("Expected MicroserviceRestClientResponseException");
        } catch (MicroserviceRestClientResponseException e) {
            assertEquals(500, e.getHttpStatus());
            assertEquals("test-value", e.getResponseHeaders().get("test-header").get(0));
            assertTrue(e.getCause() instanceof RemoteCodeException);
            RemoteCodeException remoteCodeException = (RemoteCodeException) e.getCause();
            assertEquals(tmfErrorResponse.getCode(), remoteCodeException.getErrorCode().getCode());
        } finally {
            mockBackEnd.takeRequest(60, TimeUnit.SECONDS);
        }
    }
}
