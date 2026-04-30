package com.netcracker.cloud.contexts.xchannelrequestid;

import org.junit.jupiter.api.Test;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.contexts.IncomingContextDataFactory;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextObject;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextProvider;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class XChannelRequestIdProviderApiTest {

    @Test
    void checkXChannelRequestIdContextName() {
        assertEquals("X-Channel-Request-Id", XChannelRequestIdContextProvider.X_CHANNEL_REQUEST_ID_CONTEXT_NAME);
        assertEquals("X-Channel-Request-Id", new XChannelRequestIdContextProvider().contextName());
    }

    @Test
    void xChannelRequestIdProviderMustHaveDefaultConstructor() {
        XChannelRequestIdContextProvider xChannelRequestIdContextProvider = new XChannelRequestIdContextProvider();
        assertNotNull(xChannelRequestIdContextProvider);
    }

    @Test
    void xChannelRequestIdProvideMethodWithIncomingContextData() {
        XChannelRequestIdContextProvider xChannelRequestIdContextProvider = new XChannelRequestIdContextProvider();
        IncomingContextData xChannelRequestIdIncomingContextData = IncomingContextDataFactory.getXChannelRequestIdIncomingContextData();
        XChannelRequestIdContextObject xChannelRequestIdContextObject = xChannelRequestIdContextProvider.provide(xChannelRequestIdIncomingContextData);

        assertEquals(xChannelRequestIdIncomingContextData.get("X-Channel-Request-Id"), xChannelRequestIdContextObject.getChannelRequestId());
    }

    @Test
    void xChannelRequestIdProvideMethodWithNullableParameter() {
        XChannelRequestIdContextProvider xChannelRequestIdContextProvider = new XChannelRequestIdContextProvider();
        XChannelRequestIdContextObject xChannelRequestIdContextObject = xChannelRequestIdContextProvider.provide(null);

        assertEquals("-", xChannelRequestIdContextObject.getChannelRequestId());
    }

    @Test
    void xChannelRequestIdProvideMethodWithIncomingContextDataWithoutXChannelRequestId() {
        XChannelRequestIdContextProvider xChannelRequestIdContextProvider = new XChannelRequestIdContextProvider();
        IncomingContextData incomingContextData = new IncomingContextData() {
            @Override
            public Object get(String name) {
                return null;
            }

            @Override
            public Map<String, List<?>> getAll() {
                return Collections.emptyMap();
            }
        };
        XChannelRequestIdContextObject xChannelRequestIdContextObject = xChannelRequestIdContextProvider.provide(incomingContextData);

        assertEquals("-", xChannelRequestIdContextObject.getChannelRequestId());
    }
}
