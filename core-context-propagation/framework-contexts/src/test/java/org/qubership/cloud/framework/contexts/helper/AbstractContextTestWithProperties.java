package org.qubership.cloud.framework.contexts.helper;

import java.util.Map;

public abstract class AbstractContextTestWithProperties {

    public static void parentSetup(Map<String, String> properties) {
        properties.forEach(System::setProperty);
        ContextPropagationTestUtils.reinitializeRegistry();
    }

    public static void parentCleanup(Map<String, String> properties) {
        properties.keySet().forEach(System::clearProperty);
        ContextPropagationTestUtils.reinitializeRegistry();
    }
}
