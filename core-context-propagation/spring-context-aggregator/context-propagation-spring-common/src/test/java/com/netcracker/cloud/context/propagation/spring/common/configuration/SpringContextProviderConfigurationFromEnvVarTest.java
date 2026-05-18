package com.netcracker.cloud.context.propagation.spring.common.configuration;

import com.netcracker.cloud.framework.contexts.allowedheaders.HeaderPropagationConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({HeaderPropagationStateReset.class, SystemStubsExtension.class, SpringExtension.class})
@ContextConfiguration(classes = SpringContextProviderConfiguration.class)
@TestPropertySource(properties = {
        // context.propagation.allow-blocked-headers deliberately NOT declared here — it must come from the env var
        "headers.allowed=custom-header"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpringContextProviderConfigurationFromEnvVarTest {

    @SystemStub
    static EnvironmentVariables envVars = new EnvironmentVariables("CONTEXT_PROPAGATION_ALLOW_BLOCKED_HEADERS", "X-Channel-Request-Id");

    @Test
    void shouldReadHeadersAllowBlockedFromEnvVar() {
        assertEquals("X-Channel-Request-Id", System.getProperty("context.propagation.allow-blocked-headers"),
                "Spring init() must propagate env-sourced value to the system property");

        HeaderPropagationConfiguration.resetCache();
        assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"),
                "Env-sourced exemption must take effect");
        assertTrue(HeaderPropagationConfiguration.blockedHeaders().isEmpty(),
                "The only entry of the internal blocklist must be removed by the exemption");
    }
}
