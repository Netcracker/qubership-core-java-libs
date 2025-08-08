package com.netcracker.cloud.framework.contexts.xrequestid;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.framework.contexts.strategies.AbstractXRequestIdStrategy;
import org.jetbrains.annotations.Nullable;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MdcXRequestIdContextTest {

    public static String X_REQUEST_ID_VALUE = "changedId123";

    private final AbstractXRequestIdStrategy strategy = new XRequestIdStrategy(() -> provide(null));

    @AfterEach
    @BeforeEach
    void cleanUp() {
        ContextManager.clearAll();
        MDC.remove(XRequestIdContextObject.X_REQUEST_ID);
    }

    @Test
    void mdcShouldPutXRequestIdFromStrategy() {
        assertEquals(strategy.get().getRequestId(),getFromMdc());
    }

    @Test
    void mdcShouldPutCustomXRequestId() {
        strategy.set(new XRequestIdContextObject(X_REQUEST_ID_VALUE));
        assertEquals(X_REQUEST_ID_VALUE, getFromMdc());
    }

    @Test
    void mdcShouldRemoveXRequestId() {
        assertEquals(strategy.get().getRequestId(), getFromMdc());
        strategy.clear();
        assertNull(getFromMdc());
    }

    public XRequestIdContextObject provide(@Nullable IncomingContextData incomingContextData) {
        return new XRequestIdContextObject(incomingContextData);
    }

    private String getFromMdc() {
        return MDC.get(AbstractXRequestIdStrategy.MDC_REQUEST_ID_KEY);
    }
}
