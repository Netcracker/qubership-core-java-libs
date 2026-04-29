package com.netcracker.cloud.framework.contexts.helper;

import java.util.Map;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.framework.contexts.allowedheaders.HeaderPropagationConfiguration;

public abstract class AbstractContextTestWithProperties {

    public static void parentSetup(Map<String, String> properties) {
        properties.forEach(System::setProperty);
        HeaderPropagationConfiguration.resetCache();
        ContextManager.reinitialize();
    }

    public static void parentCleanup(Map<String, String> properties) {
        properties.keySet().forEach(System::clearProperty);
        HeaderPropagationConfiguration.resetCache();
        ContextManager.reinitialize();
    }
}
