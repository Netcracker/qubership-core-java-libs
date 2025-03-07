package org.qubership.cloud.restlegacy.restclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;
import org.qubership.cloud.restlegacy.restclient.configuration.ClientsTestConfiguration;
import org.qubership.cloud.restlegacy.restclient.error.ErrorsDescription;
import org.qubership.cloud.restlegacy.restclient.error.ProxyErrorException;
import org.qubership.cloud.restlegacy.restclient.error.ProxyRethrowException;
import org.qubership.cloud.restlegacy.resttemplate.RestTemplateFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ClientsTestConfiguration.class)
public class RestClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Autowired
    private RetryTemplate retryTemplate;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private RestClient restClient;
    @Autowired
    private RestTemplateFactory restTemplateFactory;

    @Before
    public void setUp() {
        reset(retryTemplate, restTemplate);
        Whitebox.setInternalState(restClient, "restTemplateFactory", restTemplateFactory);
    }

    @Test
    public void testPost() throws Throwable {
        final Object objectToPost = new Object();

        restClient.post("someUrl", objectToPost);

        verify(retryTemplate).execute(any());
        verify(restTemplate).postForEntity("someUrl", objectToPost, Object.class);
    }

    @Test
    public void testPostWithResponseType() throws Throwable {
        final Object objectToPost = new Object();

        restClient.post("someUrl", objectToPost, String.class);

        verify(retryTemplate).execute(any());
        verify(restTemplate).postForEntity("someUrl", objectToPost, String.class);
    }

    @Test
    public void testGet() throws Throwable {
        restClient.get("someUrl");

        verify(retryTemplate).execute(any());
        verify(restTemplate).getForEntity("someUrl", Object.class);
    }

    @Test
    public void testGetWithResponseType() throws Throwable {
        restClient.get("someUrl", String.class);

        verify(retryTemplate).execute(any());
        verify(restTemplate).getForEntity("someUrl", String.class);
    }

    @Test
    public void testPut() throws Throwable {
        final Object objectToPut = new Object();
        restClient.put("someUrl", objectToPut);

        verify(retryTemplate).execute(any());
        verify(restTemplate).put("someUrl", objectToPut);
    }

    @Test
    public void testDelete() throws Throwable {
        restClient.delete("someUrl");

        verify(retryTemplate).execute(any());
        verify(restTemplate).delete("someUrl");
    }

    @Test
    public void testPatch() throws Throwable {
        Object request = new Object();
        restClient.patch("someUrl", request, Object.class);

        verify(retryTemplate).execute(any());
        verify(restTemplateFactory.getRestTemplate()).patchForObject("someUrl", request, Object.class);
    }

    @Test
    public void testHead() throws Throwable {
        restClient.head("someUrl");

        verify(retryTemplate).execute(any());
        verify(restTemplateFactory.getRestTemplate()).headForHeaders("someUrl");
    }

    @Test
    public void testOptions() throws Throwable {
        restClient.options("someUrl");

        verify(retryTemplate).execute(any());
        verify(restTemplateFactory.getRestTemplate()).optionsForAllow("someUrl");
    }

    @Test
    public void testExceptionIsWrappedInProxyErrorExceptionOnPost() throws Exception {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        when(restTemplate.postForEntity(anyString(), any(), any())).thenThrow(cause);

        expectedException.expect(ProxyErrorException.class);
        expectedException.expectCause(is(cause));
        restClient.post("someUrl", new Object());
    }

    @Test
    public void testExceptionIsWrappedInProxyRethrowExceptionOnPost() throws Exception {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, null, getErrorDescription().getBytes(), null);
        when(restTemplate.postForEntity(anyString(), any(), any())).thenThrow(cause);

        expectedException.expect(ProxyRethrowException.class);
        restClient.post("someUrl", new Object());
    }


    @Test
    public void testExceptionIsWrapperInProxyErrorExceptionOnGet() throws Exception {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        when(restTemplate.getForEntity(anyString(), any())).thenThrow(cause);

        expectedException.expect(ProxyErrorException.class);
        expectedException.expectCause(is(cause));
        restClient.get("someUrl");
    }

    @Test
    public void testExceptionIsWrapperInProxyRethrowExceptionOnGet() throws Exception {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, null, getErrorDescription().getBytes(), null);
        when(restTemplate.getForEntity(anyString(), any())).thenThrow(cause);

        expectedException.expect(ProxyRethrowException.class);
        restClient.get("someUrl");
    }

    @Test
    public void testExceptionIsWrappedInProxyErrorExceptionOnPut() throws Exception {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        doThrow(cause).when(restTemplate).put(anyString(), any());

        expectedException.expect(ProxyErrorException.class);
        expectedException.expectCause(is(cause));
        restClient.put("someUrl", new Object());
    }

    @Test
    public void testExceptionIsWrappedInProxyRethrowExceptionOnPut() throws Exception {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, null, getErrorDescription().getBytes(), null);
        doThrow(cause).when(restTemplate).put(anyString(), any());

        expectedException.expect(ProxyRethrowException.class);
        restClient.put("someUrl", new Object());
    }

    @Test
    public void testExceptionIsWrappedInProxyErrorExceptionOnDelete() throws Exception {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        doThrow(cause).when(restTemplate).delete(anyString());

        expectedException.expect(ProxyErrorException.class);
        expectedException.expectCause(is(cause));
        restClient.delete("someUrl");
    }

    @Test
    public void testExceptionIsWrappedInProxyRethrowExceptionOnDelete() throws Exception {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, null, getErrorDescription().getBytes(), null);
        doThrow(cause).when(restTemplate).delete(anyString());

        expectedException.expect(ProxyRethrowException.class);
        restClient.delete("someUrl");
    }

    @Test
    public void testUserRestTemplate() throws Exception {
        RestTemplate clientRestTemplate = restClient.getRestTemplate();
        assertEquals(restTemplateFactory.getRestTemplate(), clientRestTemplate);
    }

    @Test
    public void testDefaultRestTemplate() throws Exception {
        RestTemplate clientRestTemplate = restClient.getRestTemplate();
        assertEquals(restTemplate, clientRestTemplate);
    }

    @Test
    public void testAddClientHttpRequestInterceptor() {
        ClientHttpRequestInterceptor testClientHttpInterceptor = (httpRequest, bytes, clientHttpRequestExecution) -> null;
        restClient.addClientHttpRequestInterceptor(testClientHttpInterceptor);

        verify(restClient.getRestTemplate(), Mockito.times(1)).getInterceptors();
        verifyNoMoreInteractions(restClient.getRestTemplate());
    }

    @Test
    public void testAddClientHttpRequestInterceptorM2MTrue() {
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