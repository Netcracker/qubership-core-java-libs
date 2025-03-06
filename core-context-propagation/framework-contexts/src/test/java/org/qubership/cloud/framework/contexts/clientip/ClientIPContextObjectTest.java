package org.qubership.cloud.framework.contexts.clientip;

import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;
import org.qubership.cloud.framework.contexts.clientip.ClientIPContextObject;
import org.qubership.cloud.headerstracking.filters.context.ClientIPContext;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.qubership.cloud.framework.contexts.clientip.ClientIPContextObject.X_FORWARDED_FOR;
import static org.qubership.cloud.framework.contexts.clientip.ClientIPContextObject.X_NC_CLIENT_IP;
import static org.junit.Assert.*;

public class ClientIPContextObjectTest {

    private static final String CLIENT_IP = "127.0.0.1";
    private static final String MULTIPLE_IPS = "127.0.0.2,127.0.0.3";
    private static final String FIRST_OF_MULTIPLE_IPS = "127.0.0.2";

    @Test
    public void testDefault() {
        ClientIPContextObject clientIPContextObject = new ClientIPContextObject(new IncomingContextDataImpl(Map.of()));
        OutgoingContextDataImpl outgoingContextData = new OutgoingContextDataImpl();
        clientIPContextObject.serialize(outgoingContextData);
        assertFalse(outgoingContextData.getResponseHeaders().containsKey(X_FORWARDED_FOR));
        assertFalse(outgoingContextData.getResponseHeaders().containsKey(X_NC_CLIENT_IP));
        assertEquals("", ClientIPContext.get());
    }

    @Test
    public void testSerializeToString_XForwardedFor() {
        ClientIPContextObject clientIPContextObject = new ClientIPContextObject(new IncomingContextDataImpl(Map.of(
                X_FORWARDED_FOR, CLIENT_IP
        )));
        OutgoingContextDataImpl outgoingContextData = new OutgoingContextDataImpl();
        clientIPContextObject.serialize(outgoingContextData);
        assertNull(outgoingContextData.getResponseHeaders().get(X_FORWARDED_FOR));
        Object o = outgoingContextData.getResponseHeaders().get(X_NC_CLIENT_IP);
        assertNotNull(o);
        assertTrue(o instanceof String);
        assertEquals(CLIENT_IP, o);
    }

    @Test
    public void testSerializeToString_XForwardedFor_Multiple() {
        ClientIPContextObject clientIPContextObject = new ClientIPContextObject(new IncomingContextDataImpl(Map.of(
                X_FORWARDED_FOR, MULTIPLE_IPS
        )));
        OutgoingContextDataImpl outgoingContextData = new OutgoingContextDataImpl();
        clientIPContextObject.serialize(outgoingContextData);
        assertNull(outgoingContextData.getResponseHeaders().get(X_FORWARDED_FOR));
        Object o = outgoingContextData.getResponseHeaders().get(X_NC_CLIENT_IP);
        assertNotNull(o);
        assertTrue(o instanceof String);
        assertEquals(FIRST_OF_MULTIPLE_IPS, o);
    }

    @Test
    public void testSerializeToString_X_NC_Client_IP() {
        ClientIPContextObject clientIPContextObject = new ClientIPContextObject(new IncomingContextDataImpl(Map.of(
                X_NC_CLIENT_IP, CLIENT_IP
        )));
        OutgoingContextDataImpl outgoingContextData = new OutgoingContextDataImpl();
        clientIPContextObject.serialize(outgoingContextData);
        assertNull(outgoingContextData.getResponseHeaders().get(X_FORWARDED_FOR));
        Object o = outgoingContextData.getResponseHeaders().get(X_NC_CLIENT_IP);
        assertNotNull(o);
        assertTrue(o instanceof String);
        assertEquals(CLIENT_IP, o);
    }

    public static class IncomingContextDataImpl implements IncomingContextData {
        Map<String, Object> requestHeaders = new HashMap<>();

        public IncomingContextDataImpl(Map<String, Object> requestHeaders) {
            this.requestHeaders.putAll(requestHeaders);
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
        private Map<String, Object> responseHeaders = new HashMap<>();

        @Override
        public void set(String name, Object values) {
            responseHeaders.put(name, values);
        }

        public Map<String, Object> getResponseHeaders() {
            return responseHeaders;
        }
    }
}