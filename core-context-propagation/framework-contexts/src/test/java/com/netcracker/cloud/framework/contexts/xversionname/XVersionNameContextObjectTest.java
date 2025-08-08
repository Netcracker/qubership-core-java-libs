package org.qubership.cloud.framework.contexts.xversionname;

import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;
import org.qubership.cloud.framework.contexts.data.SimpleIncomingContextData;
import org.junit.jupiter.api.Assertions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.qubership.cloud.framework.contexts.xversionname.XVersionNameContextObject.X_VERSION_NAME_SERIALIZATION_NAME;

class XVersionNameContextObjectTest {
    private final static String CANDIDATE = "candidate";

    @Test
    void testDefault() {
        XVersionNameContextObject xVersionContextObject = new XVersionNameContextObject(new IncomingContextDataImpl(X_VERSION_NAME_SERIALIZATION_NAME, ""));
        OutgoingContextDataImpl outgoingContextData = new OutgoingContextDataImpl();
        xVersionContextObject.serialize(outgoingContextData);
        Object o = outgoingContextData.getResponseHeaders().get(X_VERSION_NAME_SERIALIZATION_NAME);
        assertNull(o);
    }

    @Test
    void testSerializeToString() {
        XVersionNameContextObject xVersionContextObject = new XVersionNameContextObject(new IncomingContextDataImpl(X_VERSION_NAME_SERIALIZATION_NAME, CANDIDATE));
        OutgoingContextDataImpl outgoingContextData = new OutgoingContextDataImpl();
        xVersionContextObject.serialize(outgoingContextData);
        Object o = outgoingContextData.getResponseHeaders().get(X_VERSION_NAME_SERIALIZATION_NAME);
        assertNotNull(o);
        assertInstanceOf(String.class, o);
        assertEquals(CANDIDATE, o);
    }

    @Test
    void testXVersionSerializableDataFromCxtManager() {
        RequestContextPropagation.initRequestContext(new SimpleIncomingContextData(Map.of(X_VERSION_NAME_SERIALIZATION_NAME, CANDIDATE)));

        Map<String, Map<String, Object>> serializableContextData = ContextManager.getSerializableContextData();

        assertTrue(serializableContextData.containsKey(XVersionNameProvider.CONTEXT_NAME));
    }

    @Test
    void testXVersionSerializableData() {
        SimpleIncomingContextData contextData = new SimpleIncomingContextData(Map.of(X_VERSION_NAME_SERIALIZATION_NAME, CANDIDATE));
        XVersionNameContextObject xVersionContextObject = new XVersionNameContextObject(contextData);

        Map<String, Object> serializableContextData = xVersionContextObject.getSerializableContextData();

        Assertions.assertEquals(1, serializableContextData.size());
        Assertions.assertEquals(CANDIDATE, serializableContextData.get(X_VERSION_NAME_SERIALIZATION_NAME));

        XVersionNameContextObject xVersionContextObject1 = new XVersionNameContextObject(new SimpleIncomingContextData());
        Assertions.assertEquals(0, xVersionContextObject1.getSerializableContextData().size());
    }

    public static class IncomingContextDataImpl implements IncomingContextData {

        Map<String, Object> requestHeaders = new HashMap<>();

        public IncomingContextDataImpl(String headerName, Object headerValue) {
            requestHeaders.put(headerName, headerValue);
        }

        @Override
        public Object get(String name) {
            return requestHeaders.get(name);
        }

        @Override
        public Map<String, List<?>> getAll() {
            return null;
        }

    }

    public static class OutgoingContextDataImpl implements OutgoingContextData {

        private final Map<String, Object> responseHeaders = new HashMap<>();

        @Override
        public void set(String name, Object values) {
            responseHeaders.put(name, values);
        }

        public Map<String, Object> getResponseHeaders() {
            return responseHeaders;
        }
    }
}
