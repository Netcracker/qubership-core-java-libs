package com.netcracker.cloud.context.propagation.spring.common.configuration;

import com.netcracker.cloud.framework.contexts.allowedheaders.HeaderPropagationConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

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
@SpringJUnitConfig(classes = SpringContextProviderConfiguration.class)
@TestPropertySource(properties = {
        "headers.allowed=custom-header",
        "headers.blocked=X-Request-Id"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpringContextProviderConfigurationOnlyNonBlockableTest {

    @BeforeAll
    static void setup() {
        System.clearProperty("headers.blocked");
        HeaderPropagationConfiguration.resetCache();
    }

    @AfterAll
    static void teardown() {
        System.clearProperty("headers.blocked");
        HeaderPropagationConfiguration.resetCache();
    }

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
