package com.netcracker.cloud.restclient.resttemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.netcracker.cloud.core.error.rest.exception.RemoteCodeException;
import com.netcracker.cloud.core.error.rest.tmf.TmfErrorResponse;
import com.netcracker.cloud.restclient.BaseMicroserviceRestClientTest;
import com.netcracker.cloud.restclient.HttpMethod;
import com.netcracker.cloud.restclient.entity.RestClientResponseEntity;
import com.netcracker.cloud.restclient.exception.MicroserviceRestClientException;
import com.netcracker.cloud.restclient.exception.MicroserviceRestClientResponseException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class MicroserviceRestTemplateTest extends BaseMicroserviceRestClientTest {
    @BeforeEach
    void setUpBase() {
        RestTemplate restTemplate = new RestTemplate();
        restClient = new MicroserviceRestTemplate(restTemplate);
    }

    @Test
    void testDefaultRequestHeaders() throws InterruptedException {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200).setBody("Test response body"));
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200).setBody("Test response body"));

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("Test-Header-Name", "Test-Header-Value");
            return execution.execute(request, body);
        });
        restClient = new MicroserviceRestTemplate(restTemplate);

        RestClientResponseEntity<Void> response = restClient.doRequest(testUrl, HttpMethod.POST, null, null, Void.class);
        RecordedRequest recordedRequest = mockBackEnd.takeRequest(60, TimeUnit.SECONDS);
        assertEquals(HttpStatus.OK.value(), response.getHttpStatus());
        assertNotNull(recordedRequest);
        assertEquals("Test-Header-Value", recordedRequest.getHeader("Test-Header-Name"));
        assertEquals(MediaType.APPLICATION_JSON.toString(), recordedRequest.getHeader("Content-Type"));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Another-Test-Header-Name", "Another-Header-Value");

        response = restClient.doRequest(testUrl, HttpMethod.POST, HeaderUtils.toMap(httpHeaders), null, Void.class);
        recordedRequest = mockBackEnd.takeRequest(60, TimeUnit.SECONDS);
        assertEquals(HttpStatus.OK.value(), response.getHttpStatus());
        assertNotNull(recordedRequest);
        assertEquals("Test-Header-Value", recordedRequest.getHeader("Test-Header-Name"));
        assertEquals("Another-Header-Value", recordedRequest.getHeader("Another-Test-Header-Name"));
        assertEquals(MediaType.APPLICATION_JSON.toString(), recordedRequest.getHeader("Content-Type"));
    }

    @Test
    void testUnexpectedRestClientException() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplate.exchange(any(URI.class), any(org.springframework.http.HttpMethod.class), any(HttpEntity.class), any(Class.class)))
                .thenThrow(new RestClientException("Got HTTP error 400 BAD REQUEST", null));
        restClient = new MicroserviceRestTemplate(restTemplate);
        assertThrows(MicroserviceRestClientException.class, () -> restClient.doRequest(testUrl, HttpMethod.POST, null, null, Void.class));
    }

    @Test
    void testUnexpectedRestClientResponseException() {
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("test-header", "test-header-value");
        final String errorMessage = "Expected error in unit test";

        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplate.exchange(any(URI.class), any(org.springframework.http.HttpMethod.class), any(HttpEntity.class), any(Class.class)))
                .thenThrow(new RestClientResponseException("Got HTTP error 400 BAD REQUEST", 400, "Bad request",
                        httpHeaders, errorMessage.getBytes(), null));
        restClient = new MicroserviceRestTemplate(restTemplate);

        boolean gotExpectedException = false;
        try {
            restClient.doRequest(testUrl, HttpMethod.POST, null, null, Void.class);
        } catch (MicroserviceRestClientResponseException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getHttpStatus());
            assertEquals(HeaderUtils.toMap(httpHeaders), e.getResponseHeaders());
            assertEquals(errorMessage, e.getResponseBodyAsString());
            gotExpectedException = true;
        }
        assertTrue(gotExpectedException);
    }

    @Test
    void testTMFRestClientResponseException() throws Exception {
        TmfErrorResponse tmfErrorResponse = TmfErrorResponse.builder()
                .id(UUID.randomUUID().toString())
                .code("TEST")
                .reason("test reason")
                .detail("test detail")
                .status(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .type(TmfErrorResponse.TYPE_V1_0)
                .build();
        mockBackEnd.enqueue(new MockResponse()
                .setHeader("test-header", "test-value")
                .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).setBody(new ObjectMapper().writeValueAsString(tmfErrorResponse)));
        RestTemplate restTemplate = new RestTemplate();
        restClient = new MicroserviceRestTemplate(restTemplate);

        boolean gotExpectedException = false;
        try {
            restClient.doRequest(testUrl, HttpMethod.POST, null, null, Void.class);
        } catch (MicroserviceRestClientResponseException e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getHttpStatus());
            assertEquals("test-value", e.getResponseHeaders().get("test-header").get(0));
            final Throwable cause = e.getCause();
            assertNotNull(cause);
            assertTrue(cause instanceof RemoteCodeException);
            RemoteCodeException remoteCodeException = (RemoteCodeException) cause;
            assertEquals(tmfErrorResponse.getCode(), remoteCodeException.getErrorCode().getCode());
            assertEquals(tmfErrorResponse.getReason(), remoteCodeException.getErrorCode().getTitle());
            assertEquals(tmfErrorResponse.getCode(), remoteCodeException.getErrorCode().getCode());
            assertEquals((Integer) HttpStatus.INTERNAL_SERVER_ERROR.value(), remoteCodeException.getStatus());
            gotExpectedException = true;
        } finally {
            RecordedRequest request = mockBackEnd.takeRequest(60, TimeUnit.SECONDS); // take request to not affect other tests
            assertNotNull(request);
            assertTrue(gotExpectedException);
        }
    }

    @Test
    void testInvalidTMFRestClientResponse() throws Exception {
        TmfErrorResponse tmfErrorResponse = TmfErrorResponse.builder()
                .id(null)
                .code(null)
                .type(null)
                .build();
        String bodyAsString = new ObjectMapper().writeValueAsString(tmfErrorResponse);
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(httpStatus.value()).setBody(bodyAsString));
        RestTemplate restTemplate = new RestTemplate();
        restClient = new MicroserviceRestTemplate(restTemplate);

        boolean gotExpectedException = false;
        try {
            restClient.doRequest(testUrl, HttpMethod.POST, null, null, Void.class);
        } catch (MicroserviceRestClientResponseException e) {
            assertEquals(httpStatus.value(), e.getHttpStatus());
            assertEquals(bodyAsString, e.getResponseBodyAsString());
            gotExpectedException = true;
        } finally {
            RecordedRequest request = mockBackEnd.takeRequest(60, TimeUnit.SECONDS); // take request to not affect other tests
            assertNotNull(request);
            assertTrue(gotExpectedException);
        }
    }
}
