package org.qubership.cloud.contexts.allowedheaders;

import org.qubership.cloud.contexts.IncomingContextDataFactory;
import org.qubership.cloud.framework.contexts.allowedheaders.AllowedHeadersContextObject;
import org.qubership.cloud.framework.contexts.allowedheaders.AllowedHeadersProvider;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

import static org.qubership.cloud.framework.contexts.allowedheaders.AllowedHeadersProvider.HEADERS_PROPERTY;
import static org.junit.Assert.assertEquals;

public class AllowedHeadersProviderApiTest {

    @Test
    public void testAllowedHeadersContextName() {
        assertEquals("allowed_header", AllowedHeadersProvider.ALLOWED_HEADER);
        assertEquals("allowed_header", new AllowedHeadersProvider().contextName());
    }

    @Test
    public void testAllowedHeadersDefaultConstructor() {
        AllowedHeadersProvider allowedHeadersProvider = new AllowedHeadersProvider();
        Assert.assertNotNull(allowedHeadersProvider);
    }

    @Test
    public void testProvideAllowedHeadersContextObject() {
        System.setProperty(HEADERS_PROPERTY, "my-header");
        AllowedHeadersProvider allowedHeadersProvider = new AllowedHeadersProvider();
        AllowedHeadersContextObject allowedHeadersContextObject = allowedHeadersProvider.provide(IncomingContextDataFactory.getAllowedHeadersIncomingContextData());

        HashMap<String, Object> expected = new HashMap<>();
        expected.put("my-header", "my-value");
        assertEquals(expected, allowedHeadersContextObject.getHeaders());

        System.clearProperty(HEADERS_PROPERTY);
    }

    @Test
    public void testProvideAllowedHeadersContextObjectWithoutHeaders() {
        AllowedHeadersProvider allowedHeadersProvider = new AllowedHeadersProvider();
        AllowedHeadersContextObject allowedHeadersContextObject = allowedHeadersProvider.provide(IncomingContextDataFactory.getAllowedHeadersIncomingContextData());

        assertEquals(new HashMap<>(), allowedHeadersContextObject.getHeaders());
    }

}
