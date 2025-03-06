package org.qubership.cloud.restclient.webclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.cloud.core.error.rest.exception.RemoteCodeException;
import org.qubership.cloud.core.error.rest.tmf.TmfErrorResponse;
import org.qubership.cloud.restclient.BaseMicroserviceRestClientTest;
import org.qubership.cloud.restclient.HttpMethod;
import org.qubership.cloud.restclient.entity.RestClientResponseEntity;
import org.qubership.cloud.restclient.exception.MicroserviceRestClientException;
import org.qubership.cloud.restclient.exception.MicroserviceRestClientResponseException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

public class MicroserviceWebClientTest extends BaseMicroserviceRestClientTest {
    @Before
    public void setUpBase() {
        WebClient webClient = WebClient.builder().build();
        restClient = new MicroserviceWebClient(webClient);
    }

    private WebClient getWebClientMock() {
        WebClient webClient = Mockito.mock(WebClient.class);
        WebClient.Builder builderMock = Mockito.mock(WebClient.Builder.class);
        Mockito.when(builderMock.build()).thenReturn(webClient);
        Mockito.when(webClient.mutate()).thenReturn(builderMock);
        return webClient;
    }

    @Test
    public void testDefaultRequestHeaders() throws InterruptedException {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200).setBody("Test response body"));
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200).setBody("Test response body"));

        WebClient webClient = WebClient.builder()
                .defaultHeader("Test-Header-Name", "Test-Header-Value")
                .build();
        restClient = new MicroserviceWebClient(webClient);

        RestClientResponseEntity<Void> response = restClient.doRequest(testUrl, HttpMethod.POST, null, null, Void.class);
        RecordedRequest recordedRequest = mockBackEnd.takeRequest(60, TimeUnit.SECONDS);
        assertEquals(HttpStatus.OK.value(), response.getHttpStatus());
        assertNotNull(recordedRequest);
        assertEquals("Test-Header-Value", recordedRequest.getHeader("Test-Header-Name"));
        assertEquals(MediaType.APPLICATION_JSON.toString(), recordedRequest.getHeader("Content-Type"));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Another-Test-Header-Name", "Another-Header-Value");

        response = restClient.doRequest(testUrl, HttpMethod.POST, httpHeaders, null, Void.class);
        recordedRequest = mockBackEnd.takeRequest(60, TimeUnit.SECONDS);
        assertEquals(HttpStatus.OK.value(), response.getHttpStatus());
        assertNotNull(recordedRequest);
        assertEquals("Test-Header-Value", recordedRequest.getHeader("Test-Header-Name"));
        assertEquals("Another-Header-Value", recordedRequest.getHeader("Another-Test-Header-Name"));
        assertEquals(MediaType.APPLICATION_JSON.toString(), recordedRequest.getHeader("Content-Type"));
    }

    @Test(expected = MicroserviceRestClientException.class)
    public void testUnexpectedWebClientExceptionWith4Arguments() {
        WebClient webClient = getWebClientMock();
        Mockito.when(webClient.method(any(org.springframework.http.HttpMethod.class))).thenThrow(new WebClientException("test exception") {
        });
        restClient = new MicroserviceWebClient(webClient);

        restClient.doRequest(testUrl, HttpMethod.POST, null, null, Void.class);
    }

    @Test(expected = MicroserviceRestClientException.class)
    public void testUnexpectedWebClientExceptionWith5Arguments() {
        WebClient webClient = getWebClientMock();
        Mockito.when(webClient.method(any(org.springframework.http.HttpMethod.class))).thenThrow(new WebClientException("test exception") {
        });
        restClient = new MicroserviceWebClient(webClient);

        restClient.doRequest(testUrl, HttpMethod.POST, null, null, Void.class, null);
    }

    @Test
    public void testUnexpectedWebClientResponseException() {
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("test-header", "test-header-value");
        final String errorMessage = "Expected error in unit test";

        WebClient webClient = getWebClientMock();
        Mockito.when(webClient.method(any(org.springframework.http.HttpMethod.class)))
                .thenThrow(new WebClientResponseException(400, "Bad request", httpHeaders, errorMessage.getBytes(), null));
        restClient = new MicroserviceWebClient(webClient);

        boolean gotExpectedException = false;
        try {
            restClient.doRequest(testUrl, HttpMethod.POST, null, null, Void.class);
        } catch (MicroserviceRestClientResponseException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getHttpStatus());
            assertEquals(httpHeaders, e.getResponseHeaders());
            assertEquals(errorMessage, e.getResponseBodyAsString());
            gotExpectedException = true;
        }
        assertTrue(gotExpectedException);
    }

    @Test
    public void testResponseToString() {
        int httpStatus = HttpStatus.BAD_REQUEST.value();
        String responseBody = "test_body";
        String responseMessage = "test_message";
        String result = "MicroserviceRestClientResponseException{" +
                "message=" + responseMessage +
                ", httpStatus=" + httpStatus +
                ", responseBody=" + responseBody +
                '}';

        WebClient webClient = getWebClientMock();
        Mockito.when(webClient.method(any(org.springframework.http.HttpMethod.class)))
                .thenThrow(new MicroserviceRestClientResponseException(responseMessage, httpStatus, responseBody.getBytes(), new HttpHeaders()));
        restClient = new MicroserviceWebClient(webClient);

        try {
            restClient.doRequest(testUrl, HttpMethod.POST, null, null, Void.class);
        } catch (MicroserviceRestClientResponseException e) {
            assertEquals(result, e.toString());
            assertEquals(httpStatus, e.getHttpStatus());
            assertEquals(responseBody, e.getResponseBodyAsString());
            assertEquals(responseMessage, e.getDetail());
        }
    }

    @Test
    public void testTMFRestClientResponseException() throws Exception {
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
        WebClient webClient = WebClient.builder().build();
        restClient = new MicroserviceWebClient(webClient);

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
    public void testInvalidTMFRestClientResponse() throws Exception {
        TmfErrorResponse tmfErrorResponse = TmfErrorResponse.builder()
                .id(null)
                .code(null)
                .type(null)
                .build();
        String bodyAsString = new ObjectMapper().writeValueAsString(tmfErrorResponse);
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(httpStatus.value()).setBody(bodyAsString));
        WebClient webClient = WebClient.builder().build();
        restClient = new MicroserviceWebClient(webClient);

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

    @Test(expected = IllegalArgumentException.class)
    public void testMicroserviceWebClient_NullArgumentThrowsException() {
        new MicroserviceWebClient((HttpClient) null);
    }

    @Test
    public void testMicroserviceWebClient_AppliedRetryWorks() {
        ConnectionProvider cp = ConnectionProvider.builder("conn-provider")
                .maxIdleTime(Duration.ofSeconds(10))
                .pendingAcquireTimeout(Duration.ofSeconds(20))
                .evictInBackground(Duration.ofSeconds(30)).build();
        MicroserviceWebClient mwc = new MicroserviceWebClient(HttpClient.create(cp));

        try {
            mwc.withRetry(Retry.backoff(2, Duration.ZERO))
                    .doRequest("http://d", HttpMethod.GET, new HashMap<>(), null, Void.class);
        } catch (IllegalStateException ee) {
            assertTrue(reactor.core.Exceptions.isRetryExhausted(ee));
            assertEquals("Retries exhausted: 2/2", ee.getMessage());
        }
    }
}