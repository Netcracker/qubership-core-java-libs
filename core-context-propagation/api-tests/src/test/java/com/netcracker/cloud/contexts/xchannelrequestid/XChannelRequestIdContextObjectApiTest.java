package com.netcracker.cloud.contexts.xchannelrequestid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.context.propagation.core.contexts.common.RequestProvider;
import com.netcracker.cloud.contexts.IncomingContextDataFactory;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextObject;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextProvider;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class XChannelRequestIdContextObjectApiTest {

    @BeforeEach
    void setup() {
        ContextManager.register(Collections.singletonList(new RequestProvider()));
    }

    @Test
    void testDefaultXChannelRequestId() {
        XChannelRequestIdContextObject xChannelRequestIdContextObject = new XChannelRequestIdContextObject((IncomingContextData) null);
        assertEquals("-", xChannelRequestIdContextObject.getChannelRequestId());
    }

    @Test
    void testXChannelRequestIdFromIncomingContextData() {
        IncomingContextData xChannelRequestIdIncomingContextData = IncomingContextDataFactory.getXChannelRequestIdIncomingContextData();
        XChannelRequestIdContextObject xChannelRequestIdContextObject = new XChannelRequestIdContextObject(xChannelRequestIdIncomingContextData);
        String expectedValue = (String) xChannelRequestIdIncomingContextData.get("X-Channel-Request-Id");
        assertEquals(expectedValue, xChannelRequestIdContextObject.getChannelRequestId());
    }

    @Test
    void testConstructorWithXChannelRequestIdParameter() {
        String customChannelRequestId = UUID.randomUUID().toString();
        XChannelRequestIdContextObject xChannelRequestIdContextObject = new XChannelRequestIdContextObject(customChannelRequestId);
        assertEquals(customChannelRequestId, xChannelRequestIdContextObject.getChannelRequestId());
    }

    @Test
    void testGetXChannelRequestIdFromContextManager() {
        // data from context
        ContextManager.register(Collections.singletonList(new XChannelRequestIdContextProvider()));
        IncomingContextData xChannelRequestIdIncomingContextData = IncomingContextDataFactory.getXChannelRequestIdIncomingContextData();
        RequestContextPropagation.initRequestContext(xChannelRequestIdIncomingContextData);
        XChannelRequestIdContextObject xChannelRequestIdContextObject = ContextManager.get(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME);
    
        assertEquals(xChannelRequestIdIncomingContextData.get("X-Channel-Request-Id"), xChannelRequestIdContextObject.getChannelRequestId());
    
        // No data, default placeholder "-"
        RequestContextPropagation.initRequestContext(null);
        xChannelRequestIdContextObject = ContextManager.get(XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME);
    
        assertEquals("-", xChannelRequestIdContextObject.getChannelRequestId());
    }
}
