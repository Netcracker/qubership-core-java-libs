package org.qubership.cloud.context.propagation.quarkus.runtime.microprofile.context;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.eclipse.microprofile.context.spi.ThreadContextController;

import java.util.Map;

class CoreThreadContextController implements ThreadContextController {
    private final Map<String, Object> restorationSource;

    public CoreThreadContextController(Map<String, Object> restorationSource) {
        this.restorationSource = restorationSource;
    }

    @Override
    public void endContext() throws IllegalStateException {
        ContextManager.activateContextSnapshot(restorationSource);
    }
}
