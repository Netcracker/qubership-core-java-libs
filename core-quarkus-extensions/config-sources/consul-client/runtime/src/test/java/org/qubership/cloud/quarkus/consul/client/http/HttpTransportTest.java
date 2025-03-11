package org.qubership.cloud.quarkus.consul.client.http;

import org.qubership.cloud.quarkus.consul.client.model.GetValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HttpTransportTest {

    private HttpClient httpClient;
    private HttpTransport httpTransport;

    @BeforeEach
    void setUp(){
        httpClient = mock(HttpClient.class);
        httpTransport = new HttpTransport(httpClient);
    }

    @Test
    void testMakeGetRequest_success() throws Exception {
        String url = "http://localhost:8500/v1/kv/test";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);


        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("[]");
        when(mockResponse.headers()).thenReturn(HttpHeaders.of(Map.of(
                        "X-Consul-Index", List.of("100"),
                        "X-Consul-Knownleader", List.of("true"),
                        "X-Consul-Lastcontact", List.of("50")
                ),
                (k, v) -> true ));

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);
        Response<List<GetValue>> response = httpTransport.makeGetRequest(url);

        assertEquals(100, response.getConsulIndex());
        assertNotNull(response);
    }

    @Test
    void testMakeGetRequest_404() throws IOException, InterruptedException {
        String url = "http://localhost:8500/v1/kv/test";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        when(mockResponse.headers()).thenReturn(HttpHeaders.of(Map.of(
                        "X-Consul-Index", List.of("100"),
                        "X-Consul-Knownleader", List.of("true"),
                        "X-Consul-Lastcontact", List.of("50")
                ),
                (k, v) -> true ));

        Response<List<GetValue>> response = httpTransport.makeGetRequest(url);

        assertNull(response.getValue());
        assertNotNull(response);
    }
}
