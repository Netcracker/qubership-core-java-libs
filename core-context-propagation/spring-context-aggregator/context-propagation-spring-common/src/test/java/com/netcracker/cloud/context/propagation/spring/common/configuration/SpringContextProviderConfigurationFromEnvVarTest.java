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

/**
 * Spring integration scenario: {@code headers.blocked} is configured only via the
 * {@code HEADERS_BLOCKED} environment variable. Verifies that Spring's relaxed
 * binding picks up the env var as the {@code headers.blocked} property, that
 * {@link SpringContextProviderConfiguration#init()} propagates it to the system
 * property, and that the downstream blocked list reflects the env-sourced value.
 *
 * <p>{@link SystemStubsExtension} must be registered before {@link SpringExtension}
 * so that the env var is set before Spring's {@code SystemEnvironmentPropertySource}
 * is consulted during context initialization.</p>
 */
@ExtendWith({HeaderPropagationStateReset.class, SystemStubsExtension.class, SpringExtension.class})
@ContextConfiguration(classes = SpringContextProviderConfiguration.class)
@TestPropertySource(properties = {
        // headers.blocked deliberately not declared here — it must come from the env var
        "headers.allowed=custom-header"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpringContextProviderConfigurationFromEnvVarTest {

    @SystemStub
    static EnvironmentVariables envVars = new EnvironmentVariables("HEADERS_BLOCKED", "Custom-Header");

    @Test
    void shouldReadHeadersBlockedFromEnvVar() {
        assertEquals("Custom-Header", System.getProperty("headers.blocked"),
                "Spring init() must propagate env-sourced value to the system property");

        HeaderPropagationConfiguration.resetCache();
        assertTrue(HeaderPropagationConfiguration.isBlacklisted("Custom-Header"),
                "env-sourced header must be blocked");
        assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"),
                "explicit env-sourced configuration overrides the default");
    }
}
