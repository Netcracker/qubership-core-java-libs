package com.netcracker.cloud.context.propagation.spring.resttemplate.interceptor;


import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.headerstracking.filters.context.AcceptLanguageContext;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class SpringRestTemplateInterceptorTest {

    @Test
    void testRestTemplateInterceptorWithAcceptLanguage() throws IOException {
        AcceptLanguageContext.set("ru");
        HttpRequest request = Mockito.mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(new HttpHeaders());

        SpringRestTemplateInterceptor interceptor = new SpringRestTemplateInterceptor();
        ClientHttpResponse response = interceptor.intercept(request, new byte[]{1}, new RequestExecution());
        assertTrue(response.getHeaders().containsKey(HttpHeaders.ACCEPT_LANGUAGE));
        assertEquals(Collections.singletonList("ru"), response.getHeaders().get(HttpHeaders.ACCEPT_LANGUAGE));
    }

    @Test
    void testMissingAcceptLanguageInterceptor() throws IOException {
        ContextManager.clearAll();
        HttpRequest request = Mockito.mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(new HttpHeaders());

        SpringRestTemplateInterceptor interceptor = new SpringRestTemplateInterceptor();
        ClientHttpResponse response = interceptor.intercept(request, new byte[]{1}, new RequestExecution());
        assertFalse(response.getHeaders().containsKey(HttpHeaders.ACCEPT_LANGUAGE));
    }

    static class RequestExecution implements ClientHttpRequestExecution {

        @Override
        public ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException {
            return new ClientHttpResponse() {
                @Override
                public HttpStatus getStatusCode() throws IOException {
                    return HttpStatus.OK;
                }

                @Override
                public int getRawStatusCode() throws IOException {
                    return 0;
                }

                @Override
                public String getStatusText() throws IOException {
                    return "OK";
                }

                @Override
                public void close() {

                }

                @Override
                public InputStream getBody() throws IOException {
                    return null;
                }

                @Override
                public HttpHeaders getHeaders() {
                    HttpHeaders headers = new HttpHeaders();
                    headers.addAll(request.getHeaders());
                    return headers;
                }
            };
        }

    }

}
