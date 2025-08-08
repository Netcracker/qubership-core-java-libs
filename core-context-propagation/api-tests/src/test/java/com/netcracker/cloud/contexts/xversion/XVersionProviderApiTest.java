package org.qubership.cloud.contexts.xversion;

import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.contexts.IncomingContextDataFactory;
import org.qubership.cloud.framework.contexts.xversion.XVersionContextObject;
import org.qubership.cloud.framework.contexts.xversion.XVersionProvider;

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
