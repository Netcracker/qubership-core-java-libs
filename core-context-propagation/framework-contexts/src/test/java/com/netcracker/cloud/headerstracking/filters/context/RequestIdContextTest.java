package com.netcracker.cloud.headerstracking.filters.context;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestIdContextTest extends AbstractContextTest {

    @Test
    void testRequestWithoutHeader() {
        assertNotNull(RequestIdContext.get());
        RequestIdContext.set("new_request_id");
        assertEquals("new_request_id", RequestIdContext.get());
    }

    @Test
    void testClearContext() {
        RequestIdContext.set("new_request_id");
        assertEquals("new_request_id", RequestIdContext.get());
        RequestIdContext.clear();
        assertNotEquals("new_request_id", RequestIdContext.get());
        String oldRequestId = RequestIdContext.get();
        assertNotNull(oldRequestId);
        RequestIdContext.clear();
        assertNotEquals(oldRequestId, RequestIdContext.get());
    }
}
