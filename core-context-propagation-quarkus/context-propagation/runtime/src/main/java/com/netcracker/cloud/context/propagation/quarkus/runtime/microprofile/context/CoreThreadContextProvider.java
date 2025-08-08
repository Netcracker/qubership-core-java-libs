package com.netcracker.cloud.context.propagation.quarkus.runtime.microprofile.context;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import java.util.Map;

public class CoreThreadContextProvider implements ThreadContextProvider {

    public static final String CORE_THREAD_CONTEXT_TYPE = "CORE_CONTEXT_PROPAGATION_INTEGRATION";

    // Empty constructor is mandatory for ServiceLoader
    public CoreThreadContextProvider() { }

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        return new CoreThreadContextSnapshot(ContextManager.createContextSnapshot());
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return new CoreEmptyThreadContextSnapshot();
    }

    @Override
    public String getThreadContextType() {
        return CORE_THREAD_CONTEXT_TYPE;
    }
}
