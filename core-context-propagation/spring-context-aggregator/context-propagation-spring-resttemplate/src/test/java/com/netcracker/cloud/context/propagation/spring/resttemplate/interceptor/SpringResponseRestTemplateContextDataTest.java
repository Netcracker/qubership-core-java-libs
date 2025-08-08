package org.qubership.cloud.context.propagation.spring.resttemplate.interceptor;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class SpringResponseRestTemplateContextDataTest {
    SpringResponseRestTemplateContextData restTemplateContextData;

    private static final String PASCAL_CASE_HEADER_KEY = "PascalCaseHeaderKey";
    private static final String PASCAL_CASE_HEADER_VALUE = "pascalCaseHeaderValue";

    @BeforeEach
    void init() {
        restTemplateContextData = new SpringResponseRestTemplateContextData();
        restTemplateContextData.set("header", "value");
        restTemplateContextData.set("list_header", Arrays.asList("first", "second", "third"));
    }

    @Test
    void testSetAndGetResponseHeaders() {
        assertEquals(2, restTemplateContextData.getResponseHeaders().size());
        assertTrue(restTemplateContextData.getResponseHeaders().containsKey("header"));
        assertEquals("value", restTemplateContextData.getResponseHeaders().get("header"));
    }

    @Test
    void testAddHeadersToRequest() {
        HttpRequest request = Mockito.mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add("name", "tmp_val");
        when(request.getHeaders()).thenReturn(headers);

        restTemplateContextData.addHeadersToRequest(request);
        assertEquals(3, request.getHeaders().size());
    }

    @Test
    void testHeadersCaseInsensitive() {
        restTemplateContextData.set(PASCAL_CASE_HEADER_KEY, PASCAL_CASE_HEADER_VALUE);

        assertTrue(restTemplateContextData.getResponseHeaders().containsKey(PASCAL_CASE_HEADER_KEY.toLowerCase()));
        assertEquals(PASCAL_CASE_HEADER_VALUE, restTemplateContextData.getResponseHeaders().get(PASCAL_CASE_HEADER_KEY.toLowerCase()));
    }

}