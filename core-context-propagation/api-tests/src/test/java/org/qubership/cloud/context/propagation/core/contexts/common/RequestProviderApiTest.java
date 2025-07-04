package org.qubership.cloud.context.propagation.core.contexts.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.contexts.IncomingContextDataFactory;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.qubership.cloud.context.propagation.core.contexts.common.RequestProvider.REQUEST_CONTEXT_NAME;

public class RequestProviderApiTest {
    @BeforeEach
    public void setup() {
        ContextManager.register(Collections.singletonList(new RequestProvider()));
    }


    @Test
    public void testDefaultConstructor() {
        final RequestProvider requestProvider = new RequestProvider();
        assertNotNull(requestProvider);
    }

    @Test
    public void testRequestContextName() {
        assertEquals("request", REQUEST_CONTEXT_NAME);
        assertEquals("request", new RequestProvider().contextName());
    }

    @Test
    public void testProvideDefaultAcceptLanguage() {
        RequestProvider requestProvider = new RequestProvider();
        final RequestContextObject requestContextObject = requestProvider.provide(null);
        assertEquals(Collections.emptyMap(), requestContextObject.getHttpHeaders());
    }

    @Test
    public void testProvideCustomAcceptLanguage() {
        RequestProvider requestProvider = new RequestProvider();
        final RequestContextObject requestContextObject = requestProvider.provide(IncomingContextDataFactory.getRequestIncomingContextData());
        assertEquals("Header-1-Value", requestContextObject.getHttpHeaders().get("Header-1".toLowerCase()).get(0));
    }
}
