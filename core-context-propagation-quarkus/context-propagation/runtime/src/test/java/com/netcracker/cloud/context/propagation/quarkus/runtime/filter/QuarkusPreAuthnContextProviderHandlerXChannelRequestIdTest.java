package com.netcracker.cloud.context.propagation.quarkus.runtime.filter;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.headerstracking.filters.context.ChannelRequestIdContext;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.RoutingContext;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuarkusPreAuthnContextProviderHandlerXChannelRequestIdTest {
    @BeforeAll
    static void init() {
        ContextManager.clearAll();
    }

    @AfterAll
    static void tearDown() {
        ContextManager.reinitialize();
    }

    @Test
    void testDoFilterWithXChannelRequestIdHeader() {
        RoutingContext routingContext = mock(RoutingContext.class);
        HttpServerRequest httpServerRequest = mock(HttpServerRequest.class);
        when(routingContext.request()).thenReturn(httpServerRequest);

        String xChannelRequestId = "X-Channel-Request-Id";
        String xChannelRequestIdValue = "channel-123";

        MultiMap multiMap = new HeadersMultiMap();
        multiMap.set(xChannelRequestId, xChannelRequestIdValue);

        when(httpServerRequest.headers()).thenReturn(multiMap);

        QuarkusPreAuthnContextProviderHandler quarkusHandler = new QuarkusPreAuthnContextProviderHandler();
        quarkusHandler.handle(routingContext);

        assertEquals(xChannelRequestIdValue, ChannelRequestIdContext.get());
    }
}
