package org.qubership.cloud.context.propagation.core.providers.xversion;

import org.qubership.cloud.context.propagation.core.Strategy;

import java.util.Optional;

public class XVersionStrategy implements Strategy<XVersionContextObject> {
    ThreadLocal<XVersionContextObject> threadLocal = new ThreadLocal<>();

    @Override
    public void clear() {
        threadLocal.remove();
    }

    @Override
    public void set(XVersionContextObject value) {
        threadLocal.set(value);
    }

    @Override
    public XVersionContextObject get() {
        return threadLocal.get();
    }

    @Override
    public Optional<XVersionContextObject> getSafe() {
        try {
            return Optional.ofNullable(get());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
