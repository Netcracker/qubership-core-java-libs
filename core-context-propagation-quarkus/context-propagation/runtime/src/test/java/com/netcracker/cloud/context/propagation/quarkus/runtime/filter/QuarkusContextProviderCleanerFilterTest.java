package com.netcracker.cloud.context.propagation.quarkus.runtime.filter;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.headerstracking.filters.context.RequestIdContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;

import java.io.IOException;

class QuarkusContextProviderCleanerFilterTest {
    @BeforeAll
    static void init() {
        ContextManager.clearAll();
    }

    @Test
    public void testMdcContextMustBeClearedOnResponseFilter() throws IOException {
        String requestId = "test-request-id-value";
        RequestIdContext.set(requestId);

        Assertions.assertEquals(requestId, RequestIdContext.get());
        Assertions.assertEquals(requestId, MDC.get("requestId"));

        QuarkusContextProviderResponseFilter filter = new QuarkusContextProviderResponseFilter();
        filter.filter(null, Mockito.mock(ContainerResponseContext.class));

        Assertions.assertNull(MDC.get("requestId"));
    }
}
