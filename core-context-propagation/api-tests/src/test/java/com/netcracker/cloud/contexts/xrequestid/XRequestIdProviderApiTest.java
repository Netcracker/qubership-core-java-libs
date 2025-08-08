package com.netcracker.cloud.contexts.xrequestid;

import org.junit.jupiter.api.Test;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.contexts.IncomingContextDataFactory;
import com.netcracker.cloud.framework.contexts.xrequestid.XRequestIdContextObject;
import com.netcracker.cloud.framework.contexts.xrequestid.XRequestIdContextProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class XRequestIdProviderApiTest {

    @Test
    void checkXRequestIdContextName() {
        assertEquals("X-Request-Id", XRequestIdContextProvider.X_REQUEST_ID_CONTEXT_NAME);
        assertEquals("X-Request-Id", new XRequestIdContextProvider().contextName());
    }

    @Test
    void xRequestIdProviderMustHaveDefaultConstructor() {
        XRequestIdContextProvider xRequestIdContextProvider = new XRequestIdContextProvider();
        assertNotNull(xRequestIdContextProvider);
    }

    @Test
    void xRequestIdProvideMethodWithIncomingContextData() {
        XRequestIdContextProvider xRequestIdContextProvider = new XRequestIdContextProvider();
        IncomingContextData xRequestIdIncomingContextData = IncomingContextDataFactory.getXRequestIdIncomingContextData();
        XRequestIdContextObject xRequestIdContextObject = xRequestIdContextProvider.provide(xRequestIdIncomingContextData);

        assertEquals(xRequestIdIncomingContextData.get("X-Request-Id"), xRequestIdContextObject.getRequestId());
    }

    @Test
    void xRequestIdProvideMethodWithNullableParameter() {
        XRequestIdContextProvider xRequestIdContextProvider = new XRequestIdContextProvider();
        XRequestIdContextObject xRequestIdContextObject = xRequestIdContextProvider.provide(null);

        assertNotNull(xRequestIdContextObject.getRequestId());
    }

}
