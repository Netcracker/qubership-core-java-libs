package com.netcracker.cloud.framework.quarkus.contexts.common;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.context.propagation.core.contexts.common.RequestContextObject;
import org.qubership.cloud.headerstracking.filters.context.AcceptLanguageContext;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class QuarkusRequestProviderTest {
    @BeforeEach
    public void clean() {
        ContextManager.clearAll();
    }

    @Test
    public void testRequestContextStoresData() {
        ContextManager.set("request", new RequestContextObject(new HashMap<String, String>() {{
            put(HttpHeaders.ACCEPT_LANGUAGE, "RU");
        }}));
        Assertions.assertNotNull(ContextManager.get("request"));
        Assertions.assertNotNull(AcceptLanguageContext.get());
        Assertions.assertEquals("RU", AcceptLanguageContext.get());
    }

    @Test
    public void testQuarkusRequestProviderIsPresent() {
        RequestContextPropagation.initRequestContext(new QuarkusContextDataRequest());
        Assertions.assertTrue(ContextManager.getContextProviders().toString().contains("QuarkusRequestProvider"));
    }

    @Test
    public void  testQuarkusRequestContextInitialization() {
        RequestContextPropagation.initRequestContext(new QuarkusContextDataRequest());
        Assertions.assertNotNull(ContextManager.get("request"));
    }
}
