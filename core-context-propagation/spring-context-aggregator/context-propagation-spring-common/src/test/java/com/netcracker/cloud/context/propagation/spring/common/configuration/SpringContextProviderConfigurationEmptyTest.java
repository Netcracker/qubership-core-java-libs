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
 * Spring integration scenario: {@code headers.blocked=} (set explicitly to empty).
 * Verifies that {@link SpringContextProviderConfiguration#init()} propagates the
 * empty value to the system property, which downstream code interprets as
 * "erase the default blocked list".
 */
@SpringJUnitConfig(classes = SpringContextProviderConfiguration.class)
@TestPropertySource(properties = {
        "headers.allowed=custom-header",
        "headers.blocked="
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpringContextProviderConfigurationEmptyTest {

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
    void shouldSetEmptySystemPropertyAndEraseDefaultBlockedList() {
        assertEquals("", System.getProperty("headers.blocked"),
                "Spring init() must propagate explicit empty value to the system property");

        HeaderPropagationConfiguration.resetCache();
        assertTrue(HeaderPropagationConfiguration.blockedHeaders().isEmpty(),
                "explicit empty value must erase the default blocked list");
        assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"),
                "X-Channel-Request-Id must no longer be blocked");
    }
}
