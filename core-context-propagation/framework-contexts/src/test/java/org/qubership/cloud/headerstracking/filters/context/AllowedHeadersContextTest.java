package org.qubership.cloud.headerstracking.filters.context;

import org.junit.Assert;
import org.junit.Test;
import org.qubership.cloud.headerstracking.filters.context.AllowedHeadersContext;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AllowedHeadersContextTest extends AbstractContextTest {

    @Test
    public void testRequestWithHeader() {
        assertEquals("custom_value", AllowedHeadersContext.getHeaders().get(CUSTOM_HEADER));
    }

    @Test
    public void testSetContext() {
        Map<String, String> resultMap = new HashMap<String, String>() {{
            put("header1", "value1");
            put("header2", "value2");
        }};
        AllowedHeadersContext.set(resultMap);
        Assert.assertEquals(resultMap, AllowedHeadersContext.getHeaders());
    }

    @Test
    public void testClearContext() {
        assertTrue(AllowedHeadersContext.getHeaders().size() > 0);
        AllowedHeadersContext.clear();
        assertEquals(0, AllowedHeadersContext.getHeaders().size());
    }
}