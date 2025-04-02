package org.qubership.cloud.context.propagation.core;

import com.foo.bar.ComSampleContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContextProviderLoaderTest {
    /**
     * Check that providers from `com.` package prefix are loaded too
     */
    @Test
    void testLoader() {
        var comPackageContext = ContextProviderLoader.loadContextProviders()
                .stream()
                .filter(provider -> provider.contextName().equals(ComSampleContext.CONTEXT_NAME))
                .findFirst()
                .orElse(null);
        assertNotNull(comPackageContext);
    }
}