package org.qubership.cloud.context.propagation.spring.webclient.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.ClientRequest;

import java.net.URI;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class SpringResponseWebClientContextDataTest {
    SpringResponseWebClientContextData webClientContextData;
    private final URI DEFAULT_URL = URI.create("http://example.com");

    private static final String PASCAL_CASE_HEADER_KEY = "PascalCaseHeaderKey";
    private static final String PASCAL_CASE_HEADER_VALUE = "pascalCaseHeaderValue";

    @BeforeEach
    public void init() {
        webClientContextData = new SpringResponseWebClientContextData();
        webClientContextData.set("header", "value");
        webClientContextData.set("list_header", Arrays.asList("first", "second", "third"));
    }

    @Test
    public void testGetResponseHeaders() {
        assertEquals(2, webClientContextData.getResponseHeaders().size());
        assertTrue(webClientContextData.getResponseHeaders().containsKey("header"));
        assertEquals("value", webClientContextData.getResponseHeaders().get("header"));
    }

    @Test
    public void testAddHeadersToRequest() {
        ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
        ClientRequest updatedRequest = webClientContextData.addHeadersToRequest(request);
        assertEquals(2, updatedRequest.headers().size());
    }

    @Test
    public void testHeadersCaseInsensitive() {
        webClientContextData.set(PASCAL_CASE_HEADER_KEY, PASCAL_CASE_HEADER_VALUE);

        assertTrue(webClientContextData.getResponseHeaders().containsKey(PASCAL_CASE_HEADER_KEY.toLowerCase()));
        assertEquals(PASCAL_CASE_HEADER_VALUE, webClientContextData.getResponseHeaders().get(PASCAL_CASE_HEADER_KEY.toLowerCase()));
    }

}