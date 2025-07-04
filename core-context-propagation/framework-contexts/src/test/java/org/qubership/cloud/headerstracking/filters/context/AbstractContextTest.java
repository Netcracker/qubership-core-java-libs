package org.qubership.cloud.headerstracking.filters.context;

import org.junit.jupiter.api.BeforeEach;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.framework.contexts.data.ContextDataRequest;

public abstract class AbstractContextTest {
    public static final String CUSTOM_HEADER = "Custom-header-1";

    @BeforeEach
    void setup() {
        System.setProperty("headers.allowed", CUSTOM_HEADER);
        RequestContextPropagation.initRequestContext(new ContextDataRequest());
    }
}