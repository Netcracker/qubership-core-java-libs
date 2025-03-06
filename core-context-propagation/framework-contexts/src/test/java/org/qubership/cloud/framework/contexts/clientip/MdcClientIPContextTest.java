package org.qubership.cloud.framework.contexts.clientip;


import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.framework.contexts.clientip.ClientIPContextObject;
import org.qubership.cloud.framework.contexts.clientip.ClientIPStrategy;
import org.qubership.cloud.framework.contexts.strategies.AbstractClientIPStrategy;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

public class MdcClientIPContextTest {
    private static final String CLIENT_IP_VALUE = "127.0.0.1";
    private final AbstractClientIPStrategy strategy = new ClientIPStrategy(() -> provide(null));

    @After
    @Before
    public void cleanUp() {
        ContextManager.clearAll();
        MDC.remove(AbstractClientIPStrategy.MDC_CLIENT_IP_KEY);
    }

    @Test
    public void mdcShouldPutClientIPFromStrategy() {
        Assert.assertEquals(strategy.get().getClientIp(), getFromMdc());
    }

    @Test
    public void mdcShouldPutCustomClientIP() {
        strategy.set(new ClientIPContextObject(CLIENT_IP_VALUE));
        Assert.assertEquals(CLIENT_IP_VALUE, getFromMdc());
    }

    @Test
    public void mdcShouldRemoveClientIP() {
        Assert.assertEquals(strategy.get().getClientIp(), getFromMdc());
        strategy.clear();
        Assert.assertNull(getFromMdc());
    }

    public ClientIPContextObject provide(@Nullable IncomingContextData incomingContextData) {
        return new ClientIPContextObject(incomingContextData);
    }

    private String getFromMdc() {
        return MDC.get(AbstractClientIPStrategy.MDC_CLIENT_IP_KEY);
    }
}
