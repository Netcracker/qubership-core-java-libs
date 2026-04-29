package com.netcracker.cloud.framework.contexts.allowedheaders;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
class HeaderPropagationConfigurationTest {
    @SystemStub
    private EnvironmentVariables environmentVariables = new EnvironmentVariables("TEST_PROP_FOR_ENV_SETUP", "1");

    @AfterEach
    void cleanup() {
        System.clearProperty(HeaderPropagationConfiguration.HEADERS_BLOCKED_PROPERTY);
    }

    @Test
    void shouldBlacklistHeaderByPropertyName() {
        System.setProperty(HeaderPropagationConfiguration.HEADERS_BLOCKED_PROPERTY, "X-Channel-Request-Id");
        HeaderPropagationConfiguration.resetCache();
        
        Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"));
        Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("x-channel-request-id"));
        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Request-Id"));
    }

    @Test
    void shouldParseCommaSeparatedBlacklistedHeaders() {
        System.setProperty(HeaderPropagationConfiguration.HEADERS_BLOCKED_PROPERTY, "X-Channel-Request-Id, X-Request-Id");
        HeaderPropagationConfiguration.resetCache();

        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Request-Id"));
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
        HeaderPropagationConfiguration.resetCache();

        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"));
        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("x-channel-request-id"));
        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Request-Id"));
    }

    @Test
    void shouldNotBlacklistXChannelRequestIdWhenOtherHeadersExplicitlyBlocked() {
        System.setProperty(HeaderPropagationConfiguration.HEADERS_BLOCKED_PROPERTY, "X-Request-Id");
        HeaderPropagationConfiguration.resetCache();

        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Request-Id"));
        Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"));
    }

    @Test
    void shouldApplyDefaultBlacklistWhenOnlyNonBlockableHeadersConfigured() {
        System.setProperty(HeaderPropagationConfiguration.HEADERS_BLOCKED_PROPERTY, "X-Request-Id, x-request-id");
        HeaderPropagationConfiguration.resetCache();

        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Request-Id"));
        Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"));
        Assertions.assertEquals(
                java.util.List.of(HeaderPropagationConfiguration.DEFAULT_BLOCKED_HEADER),
                HeaderPropagationConfiguration.blockedHeaders());
    }

    @Test
    void shouldBlacklistHeaderByEnvWhenPropertyNotSet() throws Exception {
        environmentVariables.set(HeaderPropagationConfiguration.HEADERS_BLOCKED_ENV, "X-Request-Id");
        try {
            HeaderPropagationConfiguration.resetCache();
            Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Request-Id"));
            Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"));
        } finally {
            environmentVariables.remove(HeaderPropagationConfiguration.HEADERS_BLOCKED_ENV);
            HeaderPropagationConfiguration.resetCache();
        }
    }
}
