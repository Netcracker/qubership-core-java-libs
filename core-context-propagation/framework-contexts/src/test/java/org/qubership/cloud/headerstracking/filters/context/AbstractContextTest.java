package org.qubership.cloud.headerstracking.filters.context;

import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.framework.contexts.data.ContextDataRequest;
import org.junit.Before;

public abstract class AbstractContextTest {
    public static final String CUSTOM_HEADER = "Custom-header-1";

    @Before
    public void setup() {
        System.setProperty("headers.allowed", CUSTOM_HEADER);
        RequestContextPropagation.initRequestContext(new ContextDataRequest());
    }
}