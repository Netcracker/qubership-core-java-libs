package org.qubership.cloud.contexts.xversion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contexts.common.RequestProvider;
import org.qubership.cloud.contexts.IncomingContextDataFactory;
import org.qubership.cloud.framework.contexts.xversion.XVersionContextObject;
import org.qubership.cloud.framework.contexts.xversion.XVersionProvider;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;


class XVersionContextObjectApiTest {

    private final static String XVERSION_DEFAULT_VALUE = "";

    @BeforeEach
    void setup() {
        ContextManager.register(Collections.singletonList(new RequestProvider()));
    }

    @Test
    void testDefaultXVersionValue() {
        XVersionContextObject xVersionContextObject = new XVersionContextObject(null);
        assertEquals(XVERSION_DEFAULT_VALUE, xVersionContextObject.getXVersion());
    }

    @Test
    void testXVersionSerializationName() {
        assertEquals("X-Version", XVersionContextObject.X_VERSION_SERIALIZATION_NAME);
    }

    @Test
    void testXVersionFromIncomingContextData() {
        XVersionContextObject xVersionContextObject = new XVersionContextObject(IncomingContextDataFactory.getXVersionIncomingContextData());
        assertEquals("2", xVersionContextObject.getXVersion());
    }

    @Test
    void testGetXVesrionFromContextManager() {
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
