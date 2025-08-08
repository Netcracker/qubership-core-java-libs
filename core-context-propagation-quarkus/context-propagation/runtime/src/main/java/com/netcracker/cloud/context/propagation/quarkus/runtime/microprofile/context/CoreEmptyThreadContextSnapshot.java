package org.qubership.cloud.context.propagation.quarkus.runtime.microprofile.context;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import java.util.Collections;
import java.util.Map;


public class CoreEmptyThreadContextSnapshot implements ThreadContextSnapshot {
    @Override
    public ThreadContextController begin() {
        Map<String, Object> threadContextBeforePropagation = ContextManager.createContextSnapshot();
        ContextManager.activateContextSnapshot(Collections.emptyMap());

        return new CoreThreadContextController(threadContextBeforePropagation);
    }
}
