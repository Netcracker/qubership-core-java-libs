package org.qubership.cloud.contexts.xversion;

import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.contexts.IncomingContextDataFactory;
import org.qubership.cloud.framework.contexts.xversion.XVersionContextObject;
import org.qubership.cloud.framework.contexts.xversion.XVersionProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class XVersionProviderApiTest {

    @Test
    public void checkXVersionContextName() {
        assertEquals("x-version", XVersionProvider.CONTEXT_NAME);
        assertEquals("x-version", new XVersionProvider().contextName());
    }

    @Test
    public void xVersionProviderMustHaveDefaultConstructor() {
        XVersionProvider xVersionProvider = new XVersionProvider();
        assertNotNull(xVersionProvider);
    }

    @Test
    public void xVersionProvideMethodWithIncomingContextData() {
        XVersionProvider xVersionProvider = new XVersionProvider();
        IncomingContextData xVersionIncomingContextData = IncomingContextDataFactory.getXVersionIncomingContextData();
        XVersionContextObject xVersionContextObject = xVersionProvider.provide(xVersionIncomingContextData);

        assertEquals("2", xVersionContextObject.getXVersion());
    }

    @Test
    public void xVersionProvideMethodWithNullableParameter() {
        XVersionProvider xVersionProvider = new XVersionProvider();
        XVersionContextObject xVersionContextObject = xVersionProvider.provide(null);

        assertEquals("", xVersionContextObject.getXVersion());
        assertEquals("", xVersionContextObject.getDefault());
    }

}
