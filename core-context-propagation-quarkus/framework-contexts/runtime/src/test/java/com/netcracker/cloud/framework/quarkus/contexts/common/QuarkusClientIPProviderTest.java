package com.netcracker.cloud.framework.quarkus.contexts.common;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.framework.contexts.clientip.ClientIPContextObject;
import com.netcracker.cloud.framework.contexts.strategies.AbstractClientIPStrategy;
import com.netcracker.cloud.framework.quarkus.contexts.clientip.QuarkusClientIPProvider;
import com.netcracker.cloud.headerstracking.filters.context.ClientIPContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class QuarkusClientIPProviderTest {
    @BeforeEach
    void clean() {
        ContextManager.clearAll();
    }

    @Test
    void testRequestContextStoresData() {
        String ip = "127.0.0.1";
        RequestContextPropagation.initRequestContext(new QuarkusContextDataRequest(ClientIPContextObject.X_NC_CLIENT_IP, ip));
        Assertions.assertEquals(ip, ClientIPContext.get());
        Assertions.assertEquals(ip, MDC.get(AbstractClientIPStrategy.MDC_CLIENT_IP_KEY));
    }

    @Test
    void testQuarkusRequestProviderIsPresent() {
        RequestContextPropagation.initRequestContext(new QuarkusContextDataRequest());
        Assertions.assertTrue(ContextManager.getContextProviders().toString().contains(QuarkusClientIPProvider.class.getCanonicalName()));
    }

    @Test
    void testQuarkusRequestContextInitialization() {
        RequestContextPropagation.initRequestContext(new QuarkusContextDataRequest());
        Assertions.assertEquals("", ClientIPContext.get());
        Assertions.assertEquals("", MDC.get(AbstractClientIPStrategy.MDC_CLIENT_IP_KEY));
    }

}
