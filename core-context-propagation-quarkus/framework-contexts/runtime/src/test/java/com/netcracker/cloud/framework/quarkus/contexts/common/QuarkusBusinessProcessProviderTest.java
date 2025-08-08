package com.netcracker.cloud.framework.quarkus.contexts.common;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.framework.contexts.businessprocess.BusinessProcessContextObject;
import org.qubership.cloud.framework.quarkus.contexts.businessprocess.QuarkusBusinessProcessProvider;
import org.qubership.cloud.headerstracking.filters.context.BusinessProcessIdContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class QuarkusBusinessProcessProviderTest {
    @BeforeEach
    public void clean() {
        ContextManager.clearAll();
    }

    @Test
    public void testRequestContextStoresData() {
        String uuid = UUID.randomUUID().toString();
        RequestContextPropagation.initRequestContext(new QuarkusContextDataRequest(BusinessProcessContextObject.BUSINESS_PROCESS_ID_SERIALIZATION_NAME, uuid));
        Assertions.assertEquals(uuid, BusinessProcessIdContext.get());
    }

    @Test
    public void testQuarkusRequestProviderIsPresent() {
        RequestContextPropagation.initRequestContext(new QuarkusContextDataRequest());
        Assertions.assertTrue(ContextManager.getContextProviders().toString().contains(QuarkusBusinessProcessProvider.class.getCanonicalName()));
    }

    @Test
    public void  testQuarkusRequestContextInitialization() {
        RequestContextPropagation.initRequestContext(new QuarkusContextDataRequest());
        Assertions.assertNull(BusinessProcessIdContext.get());
    }
}
