package org.qubership.cloud.context.propagation.quarkus.runtime.filter;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.headerstracking.filters.context.AcceptLanguageContext;
import org.qubership.cloud.headerstracking.filters.context.RequestIdContext;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuarkusPreAuthnContextProviderHandlerTest {
    @BeforeAll
    static void init() {
        ContextManager.clearAll();
    }

    @Test
    public void testDoFilter() {
        assertNull(AcceptLanguageContext.get());
        RoutingContext routingContext = mock(RoutingContext.class);
        HttpServerRequest httpServerRequest = mock(HttpServerRequest.class);
        when(routingContext.request()).thenReturn(httpServerRequest);

        String xRequestId = "X-Request-Id";
        String xRequestIdValue = "123";

        MultiMap multiMap = new HeadersMultiMap();
        multiMap.set(xRequestId, xRequestIdValue);

        when(httpServerRequest.headers()).thenReturn(multiMap);

        QuarkusPreAuthnContextProviderHandler quarkusHandler = new QuarkusPreAuthnContextProviderHandler();
        quarkusHandler.handle(routingContext);
        assertEquals(xRequestIdValue, RequestIdContext.get());
    }
}