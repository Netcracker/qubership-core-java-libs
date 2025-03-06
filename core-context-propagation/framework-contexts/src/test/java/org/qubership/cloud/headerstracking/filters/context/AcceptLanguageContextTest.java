package org.qubership.cloud.headerstracking.filters.context;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.contexts.common.RequestContextObject;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.Test;
import org.qubership.cloud.headerstracking.filters.context.AcceptLanguageContext;

import java.util.HashMap;

import static org.junit.Assert.*;

public class AcceptLanguageContextTest extends AbstractContextTest {

    @Test
    public void testRequestWithHeader() {
        assertEquals("en; ru;", AcceptLanguageContext.get());
        AcceptLanguageContext.set("new_lang");
        assertEquals("new_lang", AcceptLanguageContext.get());
    }

    @Test
    public void testClearContext() {
        ContextManager.set("request", new RequestContextObject(new HashMap<String, String>() {{
            put(HttpHeaders.ACCEPT_LANGUAGE, "RU");
        }}));
        assertNotNull(AcceptLanguageContext.get());
        AcceptLanguageContext.clear();
        assertEquals("RU", AcceptLanguageContext.get());
    }

    @Test
    public void testClearContextWithRequest() {
        ContextManager.set("request", new RequestContextObject(new HashMap<String, String>() {{
            put(HttpHeaders.ACCEPT_LANGUAGE, "RU");
        }}));
        assertNotNull(AcceptLanguageContext.get());
        AcceptLanguageContext.clear();
        ContextManager.clear("request");
        assertNull(AcceptLanguageContext.get());
    }
}
