package com.netcracker.cloud.context.propagation.spring.common.configuration;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.netcracker.cloud.framework.contexts.xchannelrequestid.HeaderPropagationConfiguration;

/**
 * JUnit5 extension that wipes the global JVM state touched by
 * {@code SpringContextProviderConfiguration#init()} so that adjacent test classes in the
 * same JVM do not leak {@code headers.*} system properties or the cached restricted list
 * into each other.
 */
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
        System.clearProperty(HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY);
        System.clearProperty("headers.allowed");
        HeaderPropagationConfiguration.resetCache();
    }
}
