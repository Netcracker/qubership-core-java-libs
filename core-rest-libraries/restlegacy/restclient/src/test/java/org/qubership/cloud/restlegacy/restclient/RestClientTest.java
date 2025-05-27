package org.qubership.cloud.restlegacy.restclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;
import org.qubership.cloud.restlegacy.restclient.configuration.ClientsTestConfiguration;
import org.qubership.cloud.restlegacy.restclient.error.ErrorsDescription;
import org.qubership.cloud.restlegacy.restclient.error.ProxyErrorException;
import org.qubership.cloud.restlegacy.restclient.error.ProxyRethrowException;
import org.qubership.cloud.restlegacy.resttemplate.RestTemplateFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = ClientsTestConfiguration.class)
class RestClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private RetryTemplate retryTemplate;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private RestClient restClient;
    @Autowired
    private RestTemplateFactory restTemplateFactory;

    @BeforeEach
    void setUp() {
        reset(retryTemplate, restTemplate);
        Whitebox.setInternalState(restClient, "restTemplateFactory", restTemplateFactory);
    }

    @Test
    void testPost() throws Throwable {
        final Object objectToPost = new Object();

        restClient.post("someUrl", objectToPost);

        verify(retryTemplate).execute(any());
        verify(restTemplate).postForEntity("someUrl", objectToPost, Object.class);
    }

    @Test
    void testPostWithResponseType() throws Throwable {
        final Object objectToPost = new Object();

        restClient.post("someUrl", objectToPost, String.class);

        verify(retryTemplate).execute(any());
        verify(restTemplate).postForEntity("someUrl", objectToPost, String.class);
    }

    @Test
    void testGet() throws Throwable {
        restClient.get("someUrl");

        verify(retryTemplate).execute(any());
        verify(restTemplate).getForEntity("someUrl", Object.class);
    }

    @Test
    void testGetWithResponseType() throws Throwable {
        restClient.get("someUrl", String.class);

        verify(retryTemplate).execute(any());
        verify(restTemplate).getForEntity("someUrl", String.class);
    }

    @Test
    void testPut() throws Throwable {
        final Object objectToPut = new Object();
        restClient.put("someUrl", objectToPut);

        verify(retryTemplate).execute(any());
        verify(restTemplate).put("someUrl", objectToPut);
    }

    @Test
    void testDelete() throws Throwable {
        restClient.delete("someUrl");

        verify(retryTemplate).execute(any());
        verify(restTemplate).delete("someUrl");
    }

    @Test
    void testPatch() throws Throwable {
        Object request = new Object();
        restClient.patch("someUrl", request, Object.class);

        verify(retryTemplate).execute(any());
        verify(restTemplateFactory.getRestTemplate()).patchForObject("someUrl", request, Object.class);
    }

    @Test
    void testHead() throws Throwable {
        restClient.head("someUrl");

        verify(retryTemplate).execute(any());
        verify(restTemplateFactory.getRestTemplate()).headForHeaders("someUrl");
    }

    @Test
    void testOptions() throws Throwable {
        restClient.options("someUrl");

        verify(retryTemplate).execute(any());
        verify(restTemplateFactory.getRestTemplate()).optionsForAllow("someUrl");
    }

    @Test
    void testExceptionIsWrappedInProxyErrorExceptionOnPost() throws Exception {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        when(restTemplate.postForEntity(anyString(), any(), any())).thenThrow(cause);

        Exception ex = assertThrows(ProxyErrorException.class, () -> restClient.post("someUrl", new Object()));
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testExceptionIsWrappedInProxyRethrowExceptionOnPost() throws Exception {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, null, getErrorDescription().getBytes(), null);
        when(restTemplate.postForEntity(anyString(), any(), any())).thenThrow(cause);

        assertThrows(ProxyRethrowException.class, () -> restClient.post("someUrl", new Object()));
    }


    @Test
    void testExceptionIsWrapperInProxyErrorExceptionOnGet() throws Exception {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        when(restTemplate.getForEntity(anyString(), any())).thenThrow(cause);

        Exception ex = assertThrows(ProxyErrorException.class, () -> restClient.get("someUrl"));
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testExceptionIsWrapperInProxyRethrowExceptionOnGet() throws Exception {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, null, getErrorDescription().getBytes(), null);
        when(restTemplate.getForEntity(anyString(), any())).thenThrow(cause);

        assertThrows(ProxyRethrowException.class, () -> restClient.get("someUrl"));
    }

    @Test
    void testExceptionIsWrappedInProxyErrorExceptionOnPut() throws Exception {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        doThrow(cause).when(restTemplate).put(anyString(), any());

        Exception ex = assertThrows(ProxyErrorException.class, () -> restClient.put("someUrl", new Object()));
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testExceptionIsWrappedInProxyRethrowExceptionOnPut() throws Exception {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, null, getErrorDescription().getBytes(), null);
        doThrow(cause).when(restTemplate).put(anyString(), any());

        assertThrows(ProxyRethrowException.class, () -> restClient.put("someUrl", new Object()));
    }

    @Test
    void testExceptionIsWrappedInProxyErrorExceptionOnDelete() throws Exception {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        doThrow(cause).when(restTemplate).delete(anyString());

        Exception ex = assertThrows(ProxyErrorException.class, () -> restClient.delete("someUrl"));
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testExceptionIsWrappedInProxyRethrowExceptionOnDelete() throws Exception {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, null, getErrorDescription().getBytes(), null);
        doThrow(cause).when(restTemplate).delete(anyString());

        assertThrows(ProxyRethrowException.class, () -> restClient.delete("someUrl"));
    }

    @Test
    void testUserRestTemplate() {
        RestTemplate clientRestTemplate = restClient.getRestTemplate();
        assertEquals(restTemplateFactory.getRestTemplate(), clientRestTemplate);
    }

    @Test
    void testDefaultRestTemplate() {
        RestTemplate clientRestTemplate = restClient.getRestTemplate();
        assertEquals(restTemplate, clientRestTemplate);
    }

    @Test
    void testAddClientHttpRequestInterceptor() {
        ClientHttpRequestInterceptor testClientHttpInterceptor = (httpRequest, bytes, clientHttpRequestExecution) -> null;
        restClient.addClientHttpRequestInterceptor(testClientHttpInterceptor);

        verify(restClient.getRestTemplate(), Mockito.times(1)).getInterceptors();
        verifyNoMoreInteractions(restClient.getRestTemplate());
    }

    @Test
    void testAddClientHttpRequestInterceptorM2MTrue() {
        ClientHttpRequestInterceptor testClientHttpInterceptor = (httpRequest, bytes, clientHttpRequestExecution) -> null;
        restClient.addClientHttpRequestInterceptor(testClientHttpInterceptor);

        verify(restClient.getRestTemplate(), Mockito.times(1)).getInterceptors();
        verifyNoMoreInteractions(restClient.getRestTemplate());
    }

    private String getErrorDescription() throws JsonProcessingException {
        return objectMapper.writeValueAsString(new ErrorsDescription(
                UUID.randomUUID(),
                new Date(),
                "someUrl",
                HttpStatus.SERVICE_UNAVAILABLE,
                Collections.singletonList(
                        new ErrorsDescription.ErrorDescription(
                                "Service Unavailable",
                                "Service Unavailable"
                        )
                ),
                "",
                "",
                "",
                "",
                true
        ));
    }
}
