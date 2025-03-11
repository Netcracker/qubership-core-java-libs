package org.qubership.cloud.context.propagation.quarkus.runtime.microprofile.context;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import java.util.Map;

class CoreThreadContextSnapshot implements ThreadContextSnapshot {
    private final Map<String, Object> contextSource;

    public CoreThreadContextSnapshot(Map<String, Object> contextSource) {
        this.contextSource = contextSource;
    }

    @Override
    public ThreadContextController begin() {
        Map<String, Object> threadContextBeforePropagation = ContextManager.createContextSnapshot();
        ContextManager.activateContextSnapshot(contextSource);

        return new CoreThreadContextController(threadContextBeforePropagation);
    }
}
