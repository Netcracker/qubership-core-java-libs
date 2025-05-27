package org.qubership.cloud.smartclient.rest.webclient.interceptor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.spring.webclient.interceptor.SpringWebClientInterceptor;
import org.qubership.cloud.framework.contexts.acceptlanguage.AcceptLanguageContextObject;
import org.qubership.cloud.framework.contexts.allowedheaders.AllowedHeadersContextObject;
import org.qubership.cloud.framework.contexts.xrequestid.XRequestIdContextObject;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Collections;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.qubership.cloud.framework.contexts.xrequestid.XRequestIdContextProvider.X_REQUEST_ID_CONTEXT_NAME;

class SmartClientInterceptorsTest {
    private final URI DEFAULT_URL = URI.create("http://example.com");
    private final String ALLOWED_HEADER = "allowed_header";

    @AfterEach
    void clean() {
        ContextManager.clearAll();
    }

    @Test
    void acceptLanguageInterceptorTest() {
        ContextManager.set(ACCEPT_LANGUAGE, new AcceptLanguageContextObject(new ContextDataRequest()));

        ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
        ClientResponse response = mock(ClientResponse.class);

        ExchangeFunction exchange = r -> {
            assertTrue(r.headers().containsKey(ACCEPT_LANGUAGE));
            return Mono.just(response);
        };

        SpringWebClientInterceptor acceptLanguageHeaderInterceptor = new SpringWebClientInterceptor();
        acceptLanguageHeaderInterceptor.filter(request, exchange).block();
    }

    @Test
    void acceptLanguageInterceptorMissingHeaderTest() {
        ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
        ClientResponse response = mock(ClientResponse.class);

        ExchangeFunction exchange = r -> {
            assertFalse(r.headers().containsKey(ACCEPT_LANGUAGE));
            return Mono.just(response);
        };

        SpringWebClientInterceptor acceptLanguageHeaderInterceptor = new SpringWebClientInterceptor();
        acceptLanguageHeaderInterceptor.filter(request, exchange).block();
    }

    @Test
    void requestIdHeaderInterceptorTest() {
        ContextManager.set(X_REQUEST_ID_CONTEXT_NAME, new XRequestIdContextObject(new ContextDataRequest()));

        ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
        ClientResponse response = mock(ClientResponse.class);

        ExchangeFunction exchange = r -> Mono.just(response);

        SpringWebClientInterceptor requestIdInterceptor = new SpringWebClientInterceptor();
        requestIdInterceptor.filter(request, exchange).block();
    }

    @Test
    void allowedHeadersInterceptorTest() {
        String allowedHeader = "allowed_header";
        String allowedHeaderValue = "fValue";

        ContextManager.set(ALLOWED_HEADER, new AllowedHeadersContextObject(Collections.EMPTY_MAP));
        ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).header(allowedHeader, allowedHeaderValue).build();
        ClientResponse response = mock(ClientResponse.class);

        ExchangeFunction exchange = r -> {
            assertTrue(r.headers().containsKey(allowedHeader));
            return Mono.just(response);
        };

        SpringWebClientInterceptor allowedHeadersInterceptor = new SpringWebClientInterceptor();
        allowedHeadersInterceptor.filter(request, exchange).block();
    }

    @Test
    void allowedHeadersInterceptorMissingHeadersTest() {
        ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
        ClientResponse response = mock(ClientResponse.class);

        ExchangeFunction exchange = r -> {
            assertEquals(1, r.headers().size());
            return Mono.just(response);
        };

        SpringWebClientInterceptor allowedHeadersInterceptor = new SpringWebClientInterceptor();
        allowedHeadersInterceptor.filter(request, exchange).block();
    }
}
