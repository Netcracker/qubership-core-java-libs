package org.qubership.cloud.contexts.allowedheaders;

import org.qubership.cloud.ContextPropagationHelperTest;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.context.propagation.core.contexts.common.RequestProvider;
import org.qubership.cloud.contexts.IncomingContextDataFactory;
import org.qubership.cloud.framework.contexts.allowedheaders.AllowedHeadersContextObject;
import org.qubership.cloud.framework.contexts.allowedheaders.AllowedHeadersProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.qubership.cloud.framework.contexts.allowedheaders.AllowedHeadersProvider.HEADERS_PROPERTY;
import static org.junit.Assert.assertEquals;

public class AllowedHeadersContextObjectApiTest {

    @Before
    public void setup(){
        ContextManager.register(Collections.singletonList(new RequestProvider()));
    }

    @Test
    public void testGetHeadersEmptyContextDataApi() {
        AllowedHeadersContextObject allowedHeadersContextObject = new AllowedHeadersContextObject(null, Collections.singletonList("my-header"));
        assertEquals(new HashMap<>(), allowedHeadersContextObject.getHeaders());
    }

    @Test
    public void testGetHeadersWithContextDataApi() {
        AllowedHeadersContextObject allowedHeadersContextObject = new AllowedHeadersContextObject(IncomingContextDataFactory.getAllowedHeadersIncomingContextData(),
                Collections.singletonList("my-header"));
        HashMap<String, Object> expected = new HashMap<>();
        expected.put("my-header", "my-value");
        assertEquals(expected, allowedHeadersContextObject.getHeaders());
    }

    @Test
    public void testGetHeadersWithMapApi() {
        HashMap<String, String> expected = new HashMap<>();
        expected.put("my-header", "my-value");
        AllowedHeadersContextObject allowedHeadersContextObject = new AllowedHeadersContextObject(expected);
        assertEquals(expected, allowedHeadersContextObject.getHeaders());
    }

    @Test
    public void testGetAllowedHeadersFromContextManager() {
        ContextPropagationHelperTest.clearRegistry();
        System.setProperty(HEADERS_PROPERTY, "my-header");
        ContextManager.register(Collections.singletonList(new AllowedHeadersProvider()));
        RequestContextPropagation.initRequestContext(IncomingContextDataFactory.getAllowedHeadersIncomingContextData());
        AllowedHeadersContextObject allowedHeadersContextObject = ContextManager.get(AllowedHeadersProvider.ALLOWED_HEADER); // API

        HashMap<String, String> expected = new HashMap<>();
        expected.put("my-header", "my-value");
        assertEquals(expected, allowedHeadersContextObject.getHeaders());

        System.clearProperty(HEADERS_PROPERTY);
    }
}
