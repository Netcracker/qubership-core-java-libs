package com.netcracker.cloud.context.propagation.core.contexts.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.contexts.IncomingContextDataFactory;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.qubership.cloud.context.propagation.core.contexts.common.RequestProvider.REQUEST_CONTEXT_NAME;

class RequestProviderApiTest {
    @BeforeEach
    void setup() {
        ContextManager.register(Collections.singletonList(new RequestProvider()));
    }

    @Test
    void testDefaultConstructor() {
        final RequestProvider requestProvider = new RequestProvider();
        assertNotNull(requestProvider);
    }

    @Test
    void testRequestContextName() {
        assertEquals("request", REQUEST_CONTEXT_NAME);
        assertEquals("request", new RequestProvider().contextName());
    }

    @Test
    void testProvideDefaultAcceptLanguage() {
        RequestProvider requestProvider = new RequestProvider();
        final RequestContextObject requestContextObject = requestProvider.provide(null);
        assertEquals(Collections.emptyMap(), requestContextObject.getHttpHeaders());
    }

    @Test
    void testProvideCustomAcceptLanguage() {
        RequestProvider requestProvider = new RequestProvider();
        final RequestContextObject requestContextObject = requestProvider.provide(IncomingContextDataFactory.getRequestIncomingContextData());
        assertEquals("Header-1-Value", requestContextObject.getHttpHeaders().get("Header-1".toLowerCase()).get(0));
    }
}
