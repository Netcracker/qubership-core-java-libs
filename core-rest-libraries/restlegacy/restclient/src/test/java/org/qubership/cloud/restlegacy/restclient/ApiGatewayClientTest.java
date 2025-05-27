package org.qubership.cloud.restlegacy.restclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qubership.cloud.restlegacy.restclient.configuration.ClientsTestConfiguration;
import org.qubership.cloud.restlegacy.resttemplate.RestTemplateFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = ClientsTestConfiguration.class)
class ApiGatewayClientTest {

    private static String relativeURL;
    private static String url;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ApiGatewayClient apiGatewayClient;
    @Autowired
    private RestTemplateFactory restTemplateFactory;

    @BeforeEach
    void setup() {
        relativeURL = "/relativeUrl";
        url = "http://api-gateway/api/v1/some-app" + relativeURL;
    }

    @Test
    void testPost() throws Exception {
        Object requestObject = new Object();
        when(restTemplate.postForEntity(url, requestObject, Object.class))
                .thenReturn(new ResponseEntity<Object>(new Object(), HttpStatus.OK));
        apiGatewayClient.post(relativeURL, requestObject);
        verify(restTemplate, times(1)).postForEntity(url, requestObject, Object.class);
    }

    @Test
    void testPut() throws Exception {
        Object requestObject = new Object();
        Mockito.doNothing().when(restTemplate).put(url, requestObject);
        apiGatewayClient.put(relativeURL, requestObject);
        verify(restTemplate, times(1)).put(url, requestObject);
    }


    @Test
    void testPatch() {
        Object requestObject = new Object();

        apiGatewayClient.patch(relativeURL, requestObject, Object.class);

        verify(restTemplateFactory.getRestTemplate(), times(1)).patchForObject(url, requestObject, Object.class);
    }

    @Test
    void testPostWithResponseTypeDefined() throws Exception {
        Object requestObject = new Object();
        when(restTemplate.postForEntity(url, requestObject, String.class))
                .thenReturn(new ResponseEntity<String>("Test", HttpStatus.OK));
        apiGatewayClient.post(relativeURL, requestObject, String.class);
        verify(restTemplate, times(1)).postForEntity(url, requestObject, String.class);
    }

    @Test
    void testGet() {
        when(restTemplate.getForEntity(url, Object.class))
                .thenReturn(new ResponseEntity<Object>(new Object(), HttpStatus.OK));
        apiGatewayClient.get(relativeURL);
        verify(restTemplate, times(1)).getForEntity(url, Object.class);
    }

    @Test
    void testGetWithResponseTypeDefined() throws Exception {
        when(restTemplate.getForEntity(url, String.class))
                .thenReturn(new ResponseEntity<String>("Test", HttpStatus.OK));
        apiGatewayClient.get(relativeURL, String.class);
        verify(restTemplate, times(1)).getForEntity(url, String.class);
    }

    @Test
    void testGetWithResponseTypeAndEntityDefined() throws Exception {
        HttpEntity<String> entity = new HttpEntity<>("Test body");
        when(restTemplate.exchange(url, HttpMethod.GET, entity, String.class))
                .thenReturn(new ResponseEntity<>("Test", HttpStatus.OK));
        apiGatewayClient.get(relativeURL, entity, String.class);
        verify(restTemplate, times(1)).exchange(url, HttpMethod.GET, entity, String.class);
    }

    @Test
    void testGetWith503() {
        when(restTemplate.getForEntity(url, String.class))
                .thenThrow(new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE));
        assertThrows(RestClientException.class, () -> apiGatewayClient.get(relativeURL, String.class));
    }
}
