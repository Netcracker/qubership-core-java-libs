package com.netcracker.cloud.framework.contexts.allowedheaders;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HeaderPropagationConfigurationTest {

    @BeforeEach
    void setup() {
        System.clearProperty(HeaderPropagationConfiguration.ALLOW_BLOCKED_PROPERTY);
        HeaderPropagationConfiguration.resetCache();
    }

    @AfterEach
    void cleanup() {
        System.clearProperty(HeaderPropagationConfiguration.ALLOW_BLOCKED_PROPERTY);
        HeaderPropagationConfiguration.resetCache();
    }

    @Test
    void shouldBlockXChannelRequestIdByDefault() {
        Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"));
        Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("x-channel-request-id"));
        Assertions.assertEquals(HeaderPropagationConfiguration.INTERNAL_BLOCKED_HEADERS,
                HeaderPropagationConfiguration.blockedHeaders());
    }

    @Test
    void shouldNotBlockXChannelRequestIdWhenExempted() {
        System.setProperty(HeaderPropagationConfiguration.ALLOW_BLOCKED_PROPERTY, "X-Channel-Request-Id");
        HeaderPropagationConfiguration.resetCache();

        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"));
        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("x-channel-request-id"));
        Assertions.assertTrue(HeaderPropagationConfiguration.blockedHeaders().isEmpty());
    }

    @Test
    void shouldApplyExemptionsCaseInsensitively() {
        System.setProperty(HeaderPropagationConfiguration.ALLOW_BLOCKED_PROPERTY, "x-channel-request-id");
        HeaderPropagationConfiguration.resetCache();

        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"));
    }

    @Test
    void shouldIgnoreUnknownExemptionEntries() {
        // Names that are not in the internal blocklist must not change anything.
        System.setProperty(HeaderPropagationConfiguration.ALLOW_BLOCKED_PROPERTY, "Custom-Header, X-Request-Id");
        HeaderPropagationConfiguration.resetCache();

        Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"),
                "Internal blocklist must remain unchanged when no exemption matches it");
        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("Custom-Header"));
        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Request-Id"));
    }

    @Test
    void shouldTreatEmptyExemptionPropertyAsNoExemption() {
        System.setProperty(HeaderPropagationConfiguration.ALLOW_BLOCKED_PROPERTY, "");
        HeaderPropagationConfiguration.resetCache();

        Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"));
        Assertions.assertEquals(HeaderPropagationConfiguration.INTERNAL_BLOCKED_HEADERS,
                HeaderPropagationConfiguration.blockedHeaders());
    }

    @Test
    void shouldTreatBlankAndCommaOnlyExemptionPropertyAsNoExemption() {
        System.setProperty(HeaderPropagationConfiguration.ALLOW_BLOCKED_PROPERTY, " , ,, ");
        HeaderPropagationConfiguration.resetCache();

        Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"));
    }

    @Test
    void isBlacklistedShouldReturnFalseForNullAndBlank() {
        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted(null));
        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted(""));
        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("   "));
    }
}
