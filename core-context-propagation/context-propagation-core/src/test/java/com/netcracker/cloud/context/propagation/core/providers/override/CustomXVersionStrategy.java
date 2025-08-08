package org.qubership.cloud.context.propagation.core.providers.override;

import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.providers.xversion.XVersionContextObject;

import java.util.Optional;

public class CustomXVersionStrategy implements Strategy<XVersionContextObject> {

    ThreadLocal<XVersionContextObject> threadLocal = new InheritableThreadLocal<>();



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
