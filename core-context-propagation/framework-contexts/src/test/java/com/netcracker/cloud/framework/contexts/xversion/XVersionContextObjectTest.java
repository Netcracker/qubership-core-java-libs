package com.netcracker.cloud.framework.contexts.xversion;

import org.junit.jupiter.api.Test;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.context.propagation.core.contextdata.OutgoingContextData;
import com.netcracker.cloud.framework.contexts.data.SimpleIncomingContextData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static com.netcracker.cloud.framework.contexts.xversion.XVersionContextObject.X_VERSION_SERIALIZATION_NAME;

public class XVersionContextObjectTest {
    @Test
    void testBackwardComp() {
        XVersionContextObject contextObject = new XVersionContextObject(new IncomingContextData() {
            @Override
            public Object get(String name) {
                return null;
            }

            @Override
            public Map<String, List<?>> getAll() {
                return null;
            }
        });
        assertEquals("", contextObject.getXVersion());
        assertEquals("", contextObject.getDefault());

        final IncomingContextData incomingCtx = new IncomingContextDataImpl(X_VERSION_SERIALIZATION_NAME, "v3");
        contextObject = new XVersionContextObject(incomingCtx);
        assertEquals("v3", contextObject.getXVersion());
        assertEquals("", contextObject.getDefault());
    }

    @Test
    void testSerializeToString() {
        XVersionContextObject xVersionContextObject = new XVersionContextObject(new IncomingContextDataImpl(X_VERSION_SERIALIZATION_NAME, "V1"));
        OutgoingContextDataImpl outgoingContextData = new OutgoingContextDataImpl();
        xVersionContextObject.serialize(outgoingContextData);
        Object o = outgoingContextData.getResponseHeaders().get(X_VERSION_SERIALIZATION_NAME);
        assertNotNull(o);
        assertInstanceOf(String.class, o);
    }

    @Test
    void testXVersionSerializableDataFromCxtManager() {
        RequestContextPropagation.initRequestContext(new SimpleIncomingContextData(Map.of(X_VERSION_SERIALIZATION_NAME, "1")));

        Map<String, Map<String, Object>> serializableContextData = ContextManager.getSerializableContextData();

        assertTrue(serializableContextData.containsKey(XVersionProvider.CONTEXT_NAME));
    }

    @Test
    void testXVersionSerializableData() {
        SimpleIncomingContextData contextData = new SimpleIncomingContextData(Map.of(X_VERSION_SERIALIZATION_NAME, "1"));
        XVersionContextObject xVersionContextObject = new XVersionContextObject(contextData);

        Map<String, Object> serializableContextData = xVersionContextObject.getSerializableContextData();

        assertEquals(1, serializableContextData.size());
        assertEquals("1", serializableContextData.get(X_VERSION_SERIALIZATION_NAME));

        XVersionContextObject xVersionContextObject1 = new XVersionContextObject(new SimpleIncomingContextData());
        assertEquals(0, xVersionContextObject1.getSerializableContextData().size());
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
