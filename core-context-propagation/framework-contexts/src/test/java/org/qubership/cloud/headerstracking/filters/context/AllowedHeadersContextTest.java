package org.qubership.cloud.headerstracking.filters.context;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AllowedHeadersContextTest extends AbstractContextTest {

    @Test
    void testRequestWithHeader() {
        assertEquals("custom_value", AllowedHeadersContext.getHeaders().get(CUSTOM_HEADER));
    }

    @Test
    void testSetContext() {
        Map<String, String> resultMap = new HashMap<>() {{
            put("header1", "value1");
            put("header2", "value2");
        }};
        AllowedHeadersContext.set(resultMap);
        assertEquals(resultMap, AllowedHeadersContext.getHeaders());
    }

    @Test
    void testClearContext() {
        assertFalse(AllowedHeadersContext.getHeaders().isEmpty());
        AllowedHeadersContext.clear();
        assertTrue(AllowedHeadersContext.getHeaders().isEmpty());
    }
}