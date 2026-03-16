package com.netcracker.cloud.headerstracking.filters.context;

import org.junit.jupiter.api.Test;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.contexts.common.RequestContextObject;
import jakarta.ws.rs.core.HttpHeaders;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class AcceptLanguageContextTest extends AbstractContextTest {

    @Test
    void testRequestWithHeader() {
        assertEquals("en; ru;", AcceptLanguageContext.get());
        AcceptLanguageContext.set("new_lang");
        assertEquals("new_lang", AcceptLanguageContext.get());
    }

    @Test
    void testClearContext() {
        ContextManager.set("request", new RequestContextObject(new HashMap<String, String>() {{
            put(HttpHeaders.ACCEPT_LANGUAGE, "RU");
        }}));
        assertNotNull(AcceptLanguageContext.get());
        AcceptLanguageContext.clear();
        assertEquals("RU", AcceptLanguageContext.get());
    }

    @Test
    void testClearContextWithRequest() {
        ContextManager.set("request", new RequestContextObject(new HashMap<String, String>() {{
            put(HttpHeaders.ACCEPT_LANGUAGE, "RU");
        }}));
        assertNotNull(AcceptLanguageContext.get());
        AcceptLanguageContext.clear();
        ContextManager.clear("request");
        assertNull(AcceptLanguageContext.get());
    }
}
