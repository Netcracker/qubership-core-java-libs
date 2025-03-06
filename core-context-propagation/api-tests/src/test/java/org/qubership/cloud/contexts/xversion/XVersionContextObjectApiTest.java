package org.qubership.cloud.contexts.xversion;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contexts.common.RequestProvider;
import org.qubership.cloud.contexts.IncomingContextDataFactory;
import org.qubership.cloud.framework.contexts.xversion.XVersionContextObject;
import org.qubership.cloud.framework.contexts.xversion.XVersionProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class XVersionContextObjectApiTest {

    private final static String XVERSION_DEFAULT_VALUE = "";

    @Before
    public void setup() {
        ContextManager.register(Collections.singletonList(new RequestProvider()));
    }

    @Test
    public void testDefaultXVersionValue() {
        XVersionContextObject xVersionContextObject = new XVersionContextObject(null);
        assertEquals(XVERSION_DEFAULT_VALUE, xVersionContextObject.getXVersion());
    }

    @Test
    public void testXVersionSerializationName() {
        assertEquals("X-Version", XVersionContextObject.X_VERSION_SERIALIZATION_NAME);
    }

    @Test
    public void testXVersionFromIncomingContextData() {
        XVersionContextObject xVersionContextObject = new XVersionContextObject(IncomingContextDataFactory.getXVersionIncomingContextData());
        assertEquals("2", xVersionContextObject.getXVersion());
    }

    @Test
    public void testGetXVesrionFromContextManager() {
        ContextManager.register(Collections.singletonList(new XVersionProvider()));
        IncomingContextData xVersionIncomingContextData = IncomingContextDataFactory.getXVersionIncomingContextData();
        RequestContextPropagation.initRequestContext(xVersionIncomingContextData);
        XVersionContextObject xVersionContextObject = ContextManager.get(XVersionProvider.CONTEXT_NAME); // API

        assertEquals("2", xVersionContextObject.getXVersion());

        RequestContextPropagation.initRequestContext(null);
        xVersionContextObject = ContextManager.get(XVersionProvider.CONTEXT_NAME); // API

        assertEquals(XVERSION_DEFAULT_VALUE, xVersionContextObject.getXVersion());
    }
}
