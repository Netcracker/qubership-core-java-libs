package com.netcracker.cloud.context.propagation.spring.common.configuration;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.netcracker.cloud.framework.contexts.allowedheaders.HeaderPropagationConfiguration;

public class HeaderPropagationStateReset implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext ctx) {
        reset();
    }

    @Override
    public void afterAll(ExtensionContext ctx) {
        reset();
    }

    private static void reset() {
        System.clearProperty("context.propagation.allow-blocked-headers");
        System.clearProperty("headers.allowed");
        HeaderPropagationConfiguration.resetCache();
    }
}
