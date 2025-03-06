package org.qubership.cloud.framework.contexts.helper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractContextTestWithProperties {
    protected static final Map<String, String> propertiesToSet = new HashMap<>();

    @BeforeAll
    static void parentSetup() {
        propertiesToSet.forEach(System::setProperty);
        ContextPropagationTestUtils.reinitializeRegistry();
    }

    @AfterAll
    static void parentCleanup() {
        propertiesToSet.keySet().forEach(System::clearProperty);
        ContextPropagationTestUtils.reinitializeRegistry();
    }
}
