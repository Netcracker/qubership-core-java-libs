package com.netcracker.cloud.framework.contexts.xchannelrequestid;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.framework.contexts.strategies.AbstractXChannelRequestIdStrategy;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MdcXChannelRequestIdContextTest {

    private static final String CHANNEL_REQUEST_ID_VALUE = "ch-test-42";
    private static final String DEFAULT_VALUE = "-";

    private final AbstractXChannelRequestIdStrategy strategy =
            new XChannelRequestIdStrategy(() -> provide(null));

    @AfterEach
    @BeforeEach
    void cleanUp() {
        ContextManager.clearAll();
        MDC.remove(AbstractXChannelRequestIdStrategy.MDC_CHANNEL_REQUEST_ID_KEY);
    }

    @Test
    void mdcShouldPutDefaultPlaceholderWhenNoHeaderProvided() {
        assertEquals(DEFAULT_VALUE, strategy.get().getChannelRequestId());
        assertEquals(DEFAULT_VALUE, getFromMdc());
    }

    @Test
    void mdcShouldPutCustomChannelRequestId() {
        strategy.set(new XChannelRequestIdContextObject(CHANNEL_REQUEST_ID_VALUE));
        assertEquals(CHANNEL_REQUEST_ID_VALUE, getFromMdc());
    }

    @Test
    void mdcShouldRemoveChannelRequestId() {
        strategy.set(new XChannelRequestIdContextObject(CHANNEL_REQUEST_ID_VALUE));
        assertEquals(CHANNEL_REQUEST_ID_VALUE, getFromMdc());
        strategy.clear();
        assertNull(getFromMdc());
    }

    @Test
    void mdcValueShouldMatchContextValue() {
        strategy.set(new XChannelRequestIdContextObject(CHANNEL_REQUEST_ID_VALUE));
        assertEquals(strategy.get().getChannelRequestId(), getFromMdc());
    }

    public XChannelRequestIdContextObject provide(@Nullable IncomingContextData incomingContextData) {
        return new XChannelRequestIdContextObject(incomingContextData);
    }

    private String getFromMdc() {
        return MDC.get(AbstractXChannelRequestIdStrategy.MDC_CHANNEL_REQUEST_ID_KEY);
    }
}
