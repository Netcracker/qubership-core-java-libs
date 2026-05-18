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

@ExtendWith({HeaderPropagationStateReset.class, SpringExtension.class})
@ContextConfiguration(classes = SpringContextProviderConfiguration.class)
@TestPropertySource(properties = {
        "headers.allowed=custom-header",
        "context.propagation.allow-blocked-headers=X-Channel-Request-Id"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpringContextProviderConfigurationWithValueTest {

    @Test
    void shouldExemptListedHeaderFromInternalBlocklist() {
        assertEquals("X-Channel-Request-Id", System.getProperty("context.propagation.allow-blocked-headers"));

        HeaderPropagationConfiguration.resetCache();
        assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"),
                "Exempted header must not be blocked");
        assertTrue(HeaderPropagationConfiguration.blockedHeaders().isEmpty(),
                "The only entry of the internal blocklist (X-Channel-Request-Id) must be removed");
    }
}
