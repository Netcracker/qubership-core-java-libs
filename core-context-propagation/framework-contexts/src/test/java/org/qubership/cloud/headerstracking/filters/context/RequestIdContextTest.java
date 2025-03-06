package org.qubership.cloud.headerstracking.filters.context;

import org.junit.Test;
import org.qubership.cloud.headerstracking.filters.context.RequestIdContext;

import static org.junit.Assert.*;

public class RequestIdContextTest extends AbstractContextTest {

    @Test
    public void testRequestWithoutHeader() throws Exception {
        assertNotNull(RequestIdContext.get());
        RequestIdContext.set("new_request_id");
        assertEquals("new_request_id", RequestIdContext.get());
    }

    @Test
    public void testClearContext() throws Exception {
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