package com.netcracker.cloud.context.propagation.spring.common.configuration;

import com.netcracker.cloud.framework.contexts.allowedheaders.HeaderPropagationConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring integration scenario: {@code headers.blocked} is set to a concrete blockable header.
 * Verifies that the listed header is blocked and the default's
 * {@code X-Channel-Request-Id} entry no longer applies.
 */
@ExtendWith({HeaderPropagationStateReset.class, SpringExtension.class})
@ContextConfiguration(classes = SpringContextProviderConfiguration.class)
@TestPropertySource(properties = {
        "headers.allowed=custom-header",
        "headers.blocked=Custom-Header"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpringContextProviderConfigurationWithValueTest {

    @Test
    void shouldSetSystemPropertyAndBlockConfiguredHeader() {
        assertEquals("Custom-Header", System.getProperty("headers.blocked"));

        HeaderPropagationConfiguration.resetCache();
        assertTrue(HeaderPropagationConfiguration.isBlacklisted("Custom-Header"),
                "configured header must be blocked");
        assertTrue(HeaderPropagationConfiguration.isBlacklisted("custom-header"),
                "blocking must be case-insensitive");
        assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"),
                "explicit configuration overrides the default blocked list");
    }
}
