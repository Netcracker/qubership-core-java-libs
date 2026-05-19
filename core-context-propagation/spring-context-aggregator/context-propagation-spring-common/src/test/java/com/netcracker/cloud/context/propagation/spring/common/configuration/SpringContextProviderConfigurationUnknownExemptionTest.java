package com.netcracker.cloud.context.propagation.spring.common.configuration;

import com.netcracker.cloud.framework.contexts.allowedheaders.HeaderPropagationConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({HeaderPropagationStateReset.class, SpringExtension.class})
@ContextConfiguration(classes = SpringContextProviderConfiguration.class)
@TestPropertySource(properties = {
        "headers.allowed=custom-header",
        "context.propagation.allow-blocked-headers=Custom-Header, X-Some-Other-Header"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpringContextProviderConfigurationUnknownExemptionTest {

    @Test
    void shouldLeaveInternalBlocklistIntactWhenExemptionsDontMatch() {
        assertEquals("Custom-Header, X-Some-Other-Header",
                System.getProperty("context.propagation.allow-blocked-headers"));

        HeaderPropagationConfiguration.resetCache();
        assertTrue(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"),
                "Internal blocklist must remain intact when no exemption matches it");
        assertEquals(HeaderPropagationConfiguration.INTERNAL_BLOCKED_HEADERS,
                HeaderPropagationConfiguration.blockedHeaders());
    }
}
