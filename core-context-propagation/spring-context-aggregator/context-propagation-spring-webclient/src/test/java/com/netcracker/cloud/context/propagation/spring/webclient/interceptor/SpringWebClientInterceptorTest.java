package com.netcracker.cloud.context.propagation.spring.webclient.interceptor;

import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.headerstracking.filters.context.AcceptLanguageContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringWebClientInterceptorTest {
    private final URI DEFAULT_URL = URI.create("http://example.com");

    @Test
    void testDoFilterWithAcceptLanguage() {
        AcceptLanguageContext.set("ru");
        ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
        ClientResponse response = mock(ClientResponse.class);

        ExchangeFunction exchange = r -> {
            assertTrue(r.headers().containsKey(HttpHeaders.ACCEPT_LANGUAGE));
            assertEquals(Collections.singletonList("ru"), r.headers().get(HttpHeaders.ACCEPT_LANGUAGE));
            return Mono.just(response);
        };

        when(response.statusCode()).thenReturn(HttpStatus.OK);
        SpringWebClientInterceptor springWebClientInterceptor = new SpringWebClientInterceptor();
        springWebClientInterceptor.filter(request, exchange).block();
    }

    @Test
    void testFilterWithoutAcceptLanguage() {
        ContextManager.clearAll();
        ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
        ClientResponse response = mock(ClientResponse.class);

        ExchangeFunction exchange = r -> {
            assertFalse(r.headers().containsKey(HttpHeaders.ACCEPT_LANGUAGE));
            return Mono.just(response);
        };

        when(response.statusCode()).thenReturn(HttpStatus.OK);
        SpringWebClientInterceptor springWebClientInterceptor = new SpringWebClientInterceptor();
        springWebClientInterceptor.filter(request, exchange).block();
    }

}
