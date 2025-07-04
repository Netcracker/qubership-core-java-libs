package org.qubership.cloud.contexts.xrequestid;

import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.contexts.IncomingContextDataFactory;
import org.qubership.cloud.framework.contexts.xrequestid.XRequestIdContextObject;
import org.qubership.cloud.framework.contexts.xrequestid.XRequestIdContextProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class XRequestIdProviderApiTest {

    @Test
    public void checkXRequestIdContextName() {
        assertEquals("X-Request-Id", XRequestIdContextProvider.X_REQUEST_ID_CONTEXT_NAME);
        assertEquals("X-Request-Id", new XRequestIdContextProvider().contextName());
    }

    @Test
    public void xRequestIdProviderMustHaveDefaultConstructor() {
        XRequestIdContextProvider xRequestIdContextProvider = new XRequestIdContextProvider();
        assertNotNull(xRequestIdContextProvider);
    }

    @Test
    public void xRequestIdProvideMethodWithIncomingContextData() {
        XRequestIdContextProvider xRequestIdContextProvider = new XRequestIdContextProvider();
        IncomingContextData xRequestIdIncomingContextData = IncomingContextDataFactory.getXRequestIdIncomingContextData();
        XRequestIdContextObject xRequestIdContextObject = xRequestIdContextProvider.provide(xRequestIdIncomingContextData);

        assertEquals(xRequestIdIncomingContextData.get("X-Request-Id"), xRequestIdContextObject.getRequestId());
    }

    @Test
    public void xRequestIdProvideMethodWithNullableParameter() {
        XRequestIdContextProvider xRequestIdContextProvider = new XRequestIdContextProvider();
        XRequestIdContextObject xRequestIdContextObject = xRequestIdContextProvider.provide(null);

        assertNotNull(xRequestIdContextObject.getRequestId());
    }

}
