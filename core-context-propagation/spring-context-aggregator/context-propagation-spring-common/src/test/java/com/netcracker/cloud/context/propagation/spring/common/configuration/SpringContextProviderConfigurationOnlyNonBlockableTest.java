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
 * Spring integration scenario: {@code headers.blocked=X-Request-Id} — the configured
 * value consists exclusively of a non-blockable header. The resulting blocked list
 * must be empty, NOT the built-in default. This locks in the behavior change made
 * to {@link HeaderPropagationConfiguration} (no silent fallback to default when the
 * user explicitly configured something).
 */
@ExtendWith({HeaderPropagationStateReset.class, SpringExtension.class})
@ContextConfiguration(classes = SpringContextProviderConfiguration.class)
@TestPropertySource(properties = {
        "headers.allowed=custom-header",
        "headers.blocked=X-Request-Id"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpringContextProviderConfigurationOnlyNonBlockableTest {

    @Test
    void shouldRespectExplicitOverrideEvenWhenItFiltersToEmpty() {
        assertEquals("X-Request-Id", System.getProperty("headers.blocked"));

        HeaderPropagationConfiguration.resetCache();
        assertTrue(HeaderPropagationConfiguration.blockedHeaders().isEmpty(),
                "the resulting blocked list must be empty — the default must NOT be restored");
        assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Request-Id"),
                "X-Request-Id is non-blockable and must never be blocked");
        assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"),
                "explicit (even if filter-emptied) configuration overrides the default");
    }
}
