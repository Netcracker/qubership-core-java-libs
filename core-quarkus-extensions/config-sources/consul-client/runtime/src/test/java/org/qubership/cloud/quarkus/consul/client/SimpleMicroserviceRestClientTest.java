package org.qubership.cloud.quarkus.consul.client;

import org.qubership.cloud.restclient.HttpMethod;
import org.qubership.cloud.restclient.entity.RestClientResponseEntity;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SimpleMicroserviceRestClientTest {

    private SimpleMicroserviceRestClient microserviceRestClient;

    @Test
    void doRequest_get() {
        Client client = mock(Client.class);
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(any(Class.class))).thenReturn("test-response");
        when(builder.get()).thenReturn(response);
        when(webTarget.request()).thenReturn(builder);
        when(client.target(any(URI.class))).thenReturn(webTarget);
        microserviceRestClient = new SimpleMicroserviceRestClient(client);

        RestClientResponseEntity<String> responseEntity = microserviceRestClient.doRequest(
                UriBuilder.fromUri("http://test.url").build(),
                HttpMethod.GET,
                createTestHeader(),
                null,
                String.class
        );

        verify(builder).get();
        verify(builder).header("test-header1", "test-header1-value");
        verify(builder).header("test-header2", "test-header2-value");
        assertEquals(200, responseEntity.getHttpStatus());
        assertEquals("test-response", responseEntity.getResponseBody());
    }

    @Test
    void doRequest_post() {
        Client client = mock(Client.class);
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(any(Class.class))).thenReturn("test-response");
        when(builder.post(any())).thenReturn(response);
        when(webTarget.request()).thenReturn(builder);
        when(client.target(any(URI.class))).thenReturn(webTarget);
        microserviceRestClient = new SimpleMicroserviceRestClient(client);

        RestClientResponseEntity<String> responseEntity = microserviceRestClient.doRequest(
                UriBuilder.fromUri("http://test.url").build(),
                HttpMethod.POST,
                createTestHeader(),
                "test-req-body",
                String.class
        );

        verify(builder).header("test-header1", "test-header1-value");
        verify(builder).header("test-header2", "test-header2-value");
        verify(builder).post(Entity.entity("test-req-body", MediaType.APPLICATION_JSON));
        assertEquals(200, responseEntity.getHttpStatus());
        assertEquals("test-response", responseEntity.getResponseBody());
    }

    private Map<String, List<String>> createTestHeader() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("test-header1", Collections.singletonList("test-header1-value"));
        headers.put("test-header2", Collections.singletonList("test-header2-value"));
        headers.put("test-header3", null);
        return headers;
    }
}