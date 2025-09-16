package com.netcracker.cloud.headerstracking.filters.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.framework.contexts.data.ContextDataRequest;
import com.netcracker.cloud.framework.contexts.helper.ContextPropagationTestUtils;

public abstract class AbstractContextTest {
    public static final String CUSTOM_HEADER = "Custom-header-1";

    @BeforeEach
    void setup() {
        System.setProperty("headers.allowed", CUSTOM_HEADER);
        ContextPropagationTestUtils.reinitializeRegistry();
        RequestContextPropagation.initRequestContext(new ContextDataRequest());
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("headers.allowed");
    }
}
