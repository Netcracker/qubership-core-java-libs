package com.netcracker.cloud.context.propagation.spring.common.configuration;

import com.netcracker.cloud.framework.contexts.allowedheaders.HeaderPropagationConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring integration scenario: {@code headers.blocked} is not set anywhere.
 * Verifies that {@link SpringContextProviderConfiguration#init()} does NOT touch
 * the {@code headers.blocked} system property, so downstream code falls back to
 * the built-in default blocked list (which contains {@code X-Channel-Request-Id}).
 */
@ExtendWith({HeaderPropagationStateReset.class, SpringExtension.class})
@ContextConfiguration(classes = SpringContextProviderConfiguration.class)
@TestPropertySource(properties = {
        "headers.allowed=custom-header"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpringContextProviderConfigurationNotConfiguredTest {

    @Test
    void shouldNotSetSystemPropertyAndApplyDefaultBlockedList() {
        assertNull(System.getProperty("headers.blocked"),
                "headers.blocked must remain unset when no source configures it");

        HeaderPropagationConfiguration.resetCache();
        assertTrue(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"),
                "default blocked list must apply when nothing is configured");
    }
}
