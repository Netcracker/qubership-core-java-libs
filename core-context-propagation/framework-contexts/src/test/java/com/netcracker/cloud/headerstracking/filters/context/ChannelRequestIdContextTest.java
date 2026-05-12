package com.netcracker.cloud.headerstracking.filters.context;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChannelRequestIdContextTest extends AbstractContextTest {

    @Test
    void testChannelRequestWithoutHeader() {
        assertNotNull(ChannelRequestIdContext.get());
        ChannelRequestIdContext.set("new_channel_request_id");
        assertEquals("new_channel_request_id", ChannelRequestIdContext.get());
    }

    @Test
    void testClearChannelContext() {
        ChannelRequestIdContext.set("new_channel_request_id");
        assertEquals("new_channel_request_id", ChannelRequestIdContext.get());
        ChannelRequestIdContext.clear();
        assertNotEquals("new_channel_request_id", ChannelRequestIdContext.get());
        String oldChannelRequestId = ChannelRequestIdContext.get();
        assertNotNull(oldChannelRequestId);
    }
}