package org.qubership.cloud.framework.contexts.xrequestid;


import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.framework.contexts.strategies.AbstractXRequestIdStrategy;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.qubership.cloud.framework.contexts.xrequestid.XRequestIdContextObject;
import org.qubership.cloud.framework.contexts.xrequestid.XRequestIdStrategy;
import org.slf4j.MDC;

public class MdcXRequestIdContextTest {

    public static String X_REQUEST_ID_VALUE = "changedId123";

    private AbstractXRequestIdStrategy strategy = new XRequestIdStrategy(() -> provide(null));

    @After
    @Before
    public void cleanUp() {
        ContextManager.clearAll();
        MDC.remove(XRequestIdContextObject.X_REQUEST_ID);
    }

    @Test
    public void mdcShouldPutXRequestIdFromStrategy() throws Exception {
        Assert.assertEquals(strategy.get().getRequestId(),getFromMdc());
    }

    @Test
    public void mdcShouldPutCustomXRequestId() throws Exception {
        strategy.set(new XRequestIdContextObject(X_REQUEST_ID_VALUE));
        Assert.assertEquals(X_REQUEST_ID_VALUE, getFromMdc());
    }

    @Test
    public void mdcShouldRemoveXRequestId() throws Exception {
        Assert.assertEquals(strategy.get().getRequestId(), getFromMdc());
        strategy.clear();
        Assert.assertNull(getFromMdc());
    }

    public XRequestIdContextObject provide(@Nullable IncomingContextData incomingContextData) {
        return new XRequestIdContextObject(incomingContextData);
    }

    private String getFromMdc() {
        return MDC.get(AbstractXRequestIdStrategy.MDC_REQUEST_ID_KEY);
    }
}
