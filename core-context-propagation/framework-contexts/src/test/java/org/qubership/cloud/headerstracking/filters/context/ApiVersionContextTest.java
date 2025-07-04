package org.qubership.cloud.headerstracking.filters.context;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ApiVersionContextTest {

    private static final String DEFAULT_VALUE = "v1";

    @Test
    public void getDefaultVersionTest() {
        assertEquals(DEFAULT_VALUE, ApiVersionContext.get());
    }

    @Test
    public void setAndClearTest() {
        String v2 = "v2-test";
        ApiVersionContext.set(v2);
        assertEquals(v2, ApiVersionContext.get());
        ApiVersionContext.clear();
        assertEquals(DEFAULT_VALUE, ApiVersionContext.get());
    }
}
