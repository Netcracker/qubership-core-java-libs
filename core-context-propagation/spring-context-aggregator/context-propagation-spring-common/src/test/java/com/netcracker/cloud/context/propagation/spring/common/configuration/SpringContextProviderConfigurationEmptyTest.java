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
 * Spring integration scenario: {@code headers.blocked=} (set explicitly to empty).
 * Verifies that {@link SpringContextProviderConfiguration#init()} propagates the
 * empty value to the system property, which downstream code interprets as
 * "erase the default blocked list".
 */
@ExtendWith({HeaderPropagationStateReset.class, SpringExtension.class})
@ContextConfiguration(classes = SpringContextProviderConfiguration.class)
@TestPropertySource(properties = {
        "headers.allowed=custom-header",
        "headers.blocked="
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpringContextProviderConfigurationEmptyTest {

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
