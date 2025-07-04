package org.qubership.cloud.framework.contexts.clientip;


import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.framework.contexts.strategies.AbstractClientIPStrategy;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MdcClientIPContextTest {
    private static final String CLIENT_IP_VALUE = "127.0.0.1";
    private final AbstractClientIPStrategy strategy = new ClientIPStrategy(() -> provide(null));

    @AfterEach
    @BeforeEach
    public void cleanUp() {
        ContextManager.clearAll();
        MDC.remove(AbstractClientIPStrategy.MDC_CLIENT_IP_KEY);
    }

    @Test
    public void mdcShouldPutClientIPFromStrategy() {
        assertEquals(strategy.get().getClientIp(), getFromMdc());
    }

    @Test
    public void mdcShouldPutCustomClientIP() {
        strategy.set(new ClientIPContextObject(CLIENT_IP_VALUE));
        assertEquals(CLIENT_IP_VALUE, getFromMdc());
    }

    @Test
    public void mdcShouldRemoveClientIP() {
        assertEquals(strategy.get().getClientIp(), getFromMdc());
        strategy.clear();
        assertNull(getFromMdc());
    }

    public ClientIPContextObject provide(@Nullable IncomingContextData incomingContextData) {
        return new ClientIPContextObject(incomingContextData);
    }

    private String getFromMdc() {
        return MDC.get(AbstractClientIPStrategy.MDC_CLIENT_IP_KEY);
    }
}
