package org.qubership.cloud.framework.contexts.strategies;

import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.supports.strategies.AbstractStrategy;
import org.qubership.cloud.framework.contexts.clientip.ClientIPContextObject;
import org.slf4j.MDC;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public abstract class AbstractClientIPStrategy extends AbstractStrategy<ClientIPContextObject> {
    public abstract Strategy<ClientIPContextObject> getStrategy();

    public static final String MDC_CLIENT_IP_KEY = "subjectIP";

    private static final ClientIPContextObject DEFAULT_VALUE = new ClientIPContextObject((IncomingContextData) null);

    @Override
    public void clear() {
        getStrategy().clear();
        MDC.remove(MDC_CLIENT_IP_KEY);
    }

    @Override
    public void set(ClientIPContextObject value) {
        getStrategy().set(value);
        MDC.put(MDC_CLIENT_IP_KEY, value.getClientIp());
    }

    @Override
    public ClientIPContextObject get() {
        ClientIPContextObject xRequestIdContextObject = getStrategy().get();
        MDC.put(MDC_CLIENT_IP_KEY, xRequestIdContextObject.getClientIp());
        return xRequestIdContextObject;
    }

    @Override
    public boolean isValid(@Nullable ClientIPContextObject value) {
        return value != null;
    }

    @Override
    protected Supplier<ClientIPContextObject> defaultObjectSupplier() {
        return () -> DEFAULT_VALUE;
    }
}