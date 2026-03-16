package com.netcracker.cloud.contexts.xversion;

import org.junit.jupiter.api.Test;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.contexts.IncomingContextDataFactory;
import com.netcracker.cloud.framework.contexts.xversion.XVersionContextObject;
import com.netcracker.cloud.framework.contexts.xversion.XVersionProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class XVersionProviderApiTest {

    @Test
    void checkXVersionContextName() {
        assertEquals("x-version", XVersionProvider.CONTEXT_NAME);
        assertEquals("x-version", new XVersionProvider().contextName());
    }

    @Test
    void xVersionProviderMustHaveDefaultConstructor() {
        XVersionProvider xVersionProvider = new XVersionProvider();
        assertNotNull(xVersionProvider);
    }

    @Test
    void xVersionProvideMethodWithIncomingContextData() {
        XVersionProvider xVersionProvider = new XVersionProvider();
        IncomingContextData xVersionIncomingContextData = IncomingContextDataFactory.getXVersionIncomingContextData();
        XVersionContextObject xVersionContextObject = xVersionProvider.provide(xVersionIncomingContextData);

        assertEquals("2", xVersionContextObject.getXVersion());
    }

    @Test
    void xVersionProvideMethodWithNullableParameter() {
        XVersionProvider xVersionProvider = new XVersionProvider();
        XVersionContextObject xVersionContextObject = xVersionProvider.provide(null);

        assertEquals("", xVersionContextObject.getXVersion());
        assertEquals("", xVersionContextObject.getDefault());
    }

}
