package com.netcracker.cloud.framework.contexts.allowedheaders;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HeaderPropagationConfigurationTest {

    @AfterEach
    void cleanup() {
        System.clearProperty(HeaderPropagationConfiguration.HEADERS_BLOCKED_PROPERTY);
    }

    @Test
    void shouldBlacklistHeaderByPropertyName() {
        System.setProperty(HeaderPropagationConfiguration.HEADERS_BLOCKED_PROPERTY, "X-Channel-Request-Id");

        Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"));
        Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("x-channel-request-id"));
        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Request-Id"));
    }

    @Test
    void shouldParseCommaSeparatedBlacklistedHeaders() {
        System.setProperty(HeaderPropagationConfiguration.HEADERS_BLOCKED_PROPERTY, "X-Channel-Request-Id, X-Request-Id");

        Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("X-Request-Id"));
        Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("x-channel-request-id"));
        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("Custom-Header"));
    }

    @Test
    void shouldBlacklistXChannelRequestIdByDefault() {
        Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"));
        Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("x-channel-request-id"));
        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Request-Id"));
    }

    @Test
    void shouldNotBlacklistXChannelRequestIdWhenBlockedHeadersExplicitlyEmpty() {
        System.setProperty(HeaderPropagationConfiguration.HEADERS_BLOCKED_PROPERTY, "");

        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"));
        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("x-channel-request-id"));
        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Request-Id"));
    }

    @Test
    void shouldNotBlacklistXChannelRequestIdWhenOtherHeadersExplicitlyBlocked() {
        System.setProperty(HeaderPropagationConfiguration.HEADERS_BLOCKED_PROPERTY, "X-Request-Id");

        Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("X-Request-Id"));
        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"));
    }
}
