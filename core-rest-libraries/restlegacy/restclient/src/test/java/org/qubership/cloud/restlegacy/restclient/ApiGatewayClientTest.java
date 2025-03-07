package org.qubership.cloud.restlegacy.restclient;

import org.qubership.cloud.restlegacy.restclient.configuration.ClientsTestConfiguration;
import org.qubership.cloud.restlegacy.resttemplate.RestTemplateFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ClientsTestConfiguration.class)
public class ApiGatewayClientTest {

    private static String relativeURL;
    private static String url;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ApiGatewayClient apiGatewayClient;
    @Autowired
    private RestTemplateFactory restTemplateFactory;

    @Before
    public void setup() {
        relativeURL = "/relativeUrl";
        url = "http://api-gateway/api/v1/some-app" + relativeURL;
    }

    @Test
    public void testPost() throws Exception {
        Object requestObject = new Object();
        when(restTemplate.postForEntity(url, requestObject, Object.class))
                .thenReturn(new ResponseEntity<Object>(new Object(), HttpStatus.OK));
        apiGatewayClient.post(relativeURL, requestObject);
        verify(restTemplate, times(1)).postForEntity(url, requestObject, Object.class);
    }

    @Test
    public void testPut() throws Exception {
        Object requestObject = new Object();
        Mockito.doNothing().when(restTemplate).put(url, requestObject);
        apiGatewayClient.put(relativeURL, requestObject);
        verify(restTemplate, times(1)).put(url, requestObject);
    }


    @Test
    public void testPatch() {
        Object requestObject = new Object();

        apiGatewayClient.patch(relativeURL, requestObject, Object.class);

        verify(restTemplateFactory.getRestTemplate(), times(1)).patchForObject(url, requestObject, Object.class);
    }

    @Test
    public void testPostWithResponseTypeDefined() throws Exception {
        Object requestObject = new Object();
        when(restTemplate.postForEntity(url, requestObject, String.class))
                .thenReturn(new ResponseEntity<String>("Test", HttpStatus.OK));
        apiGatewayClient.post(relativeURL, requestObject, String.class);
        verify(restTemplate, times(1)).postForEntity(url, requestObject, String.class);
    }

    @Test
    public void testGet() {
        when(restTemplate.getForEntity(url, Object.class))
                .thenReturn(new ResponseEntity<Object>(new Object(), HttpStatus.OK));
        apiGatewayClient.get(relativeURL);
        verify(restTemplate, times(1)).getForEntity(url, Object.class);
    }

    @Test
    public void testGetWithResponseTypeDefined() throws Exception {
        when(restTemplate.getForEntity(url, String.class))
                .thenReturn(new ResponseEntity<String>("Test", HttpStatus.OK));
        apiGatewayClient.get(relativeURL, String.class);
        verify(restTemplate, times(1)).getForEntity(url, String.class);
    }

    @Test
    public void testGetWithResponseTypeAndEntityDefined() throws Exception {
        HttpEntity<String> entity = new HttpEntity<>("Test body");
        when(restTemplate.exchange(url, HttpMethod.GET, entity, String.class))
                .thenReturn(new ResponseEntity<>("Test", HttpStatus.OK));
        apiGatewayClient.get(relativeURL, entity, String.class);
        verify(restTemplate, times(1)).exchange(url, HttpMethod.GET, entity, String.class);
    }

    @Test
    public void testGetWith503() {
        when(restTemplate.getForEntity(url, String.class))
                .thenThrow(new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE));
        expectedException.expect(RestClientException.class);
        apiGatewayClient.get(relativeURL, String.class);
    }
}