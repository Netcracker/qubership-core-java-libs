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
        var providers = ContextProviderLoader.loadContextProviders();
        var comPackageContext = providers
                .stream()
                .filter(provider -> provider.contextName().equals(ComSampleContext.CONTEXT_NAME))
                .findFirst()
                .orElse(null);
        assertNotNull(comPackageContext);
        assertTrue(providers.size() > 2); // check that in contexts not only `com.` provider
    }
}