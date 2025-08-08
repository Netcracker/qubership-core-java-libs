package com.netcracker.cloud.context.propagation.quarkus.runtime.filter;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.headerstracking.filters.context.RequestIdContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class QuarkusContextProviderResponseFilterTest {
    @BeforeAll
    static void init() {
        ContextManager.clearAll();
    }

    @Test
    public void testMdcContextMustBeClearedOnResponseFilter() throws IOException {
        String requestId = "123";
        RequestIdContext.set(requestId);

        Assertions.assertEquals(requestId, RequestIdContext.get());

        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);

        ContainerResponseContext containerResponseContext = mock(ContainerResponseContext.class);
        MultivaluedMap<String, Object> containerResponseHeaders = new QuarkusMultivaluedHashMap<>();
        Mockito.when(containerResponseContext.getHeaders())
                .thenReturn(containerResponseHeaders);

        QuarkusContextProviderResponseFilter quarkusFilter = new QuarkusContextProviderResponseFilter();
        quarkusFilter.filter(containerRequestContext, containerResponseContext);

        String xRequestId = "X-Request-Id";
        assertTrue(containerResponseHeaders.containsKey(xRequestId));
        assertEquals(requestId, containerResponseHeaders.getFirst(xRequestId));
    }
}
