package com.netcracker.cloud.framework.quarkus.contexts.common;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.framework.contexts.originatingbiid.OriginatingBiIdContextObject;
import org.qubership.cloud.framework.contexts.strategies.AbstractOriginatingBiIdStrategy;
import org.qubership.cloud.framework.quarkus.contexts.originatingbiid.QuarkusOriginatingBiIdProvider;
import org.qubership.cloud.headerstracking.filters.context.OriginatingBiIdContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;

class QuarkusOriginatingBiIdProviderTest {
    @BeforeEach
    void clean() {
        ContextManager.clearAll();
    }

    @Test
    void testRequestContextStoresData() {
        String uuid = UUID.randomUUID().toString();
        RequestContextPropagation.initRequestContext(new QuarkusContextDataRequest(OriginatingBiIdContextObject.ORIGINATING_BI_ID_SERIALIZATION_NAME, uuid));
        Assertions.assertEquals(uuid, OriginatingBiIdContext.get());
        Assertions.assertEquals(uuid, MDC.get(AbstractOriginatingBiIdStrategy.MDC_REQUEST_ID_KEY));
    }

    @Test
    void testQuarkusRequestProviderIsPresent() {
        RequestContextPropagation.initRequestContext(new QuarkusContextDataRequest());
        Assertions.assertTrue(ContextManager.getContextProviders().toString().contains(QuarkusOriginatingBiIdProvider.class.getCanonicalName()));
    }

    @Test
    void testQuarkusRequestContextInitialization() {
        RequestContextPropagation.initRequestContext(new QuarkusContextDataRequest());
        Assertions.assertNull(OriginatingBiIdContext.get());
        Assertions.assertNull(MDC.get(AbstractOriginatingBiIdStrategy.MDC_REQUEST_ID_KEY));
    }
}
