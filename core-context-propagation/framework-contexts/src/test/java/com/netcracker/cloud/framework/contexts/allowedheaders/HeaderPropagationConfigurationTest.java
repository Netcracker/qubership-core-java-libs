package com.netcracker.cloud.framework.contexts.allowedheaders;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
class HeaderPropagationConfigurationTest {
    @SystemStub
    private EnvironmentVariables environmentVariables = new EnvironmentVariables("TEST_PROP_FOR_ENV_SETUP", "1");

    @BeforeEach
    void setup() {
        System.clearProperty(HeaderPropagationConfiguration.HEADERS_BLOCKED_PROPERTY);
        HeaderPropagationConfiguration.resetCache();
    }

    @AfterEach
    void cleanup() {
        System.clearProperty(HeaderPropagationConfiguration.HEADERS_BLOCKED_PROPERTY);
        HeaderPropagationConfiguration.resetCache();
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
        Assertions.assertEquals(HeaderPropagationConfiguration.DEFAULT_BLOCKED_HEADERS,
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

    @Test
    void shouldNotBlockXRequestIdEvenWhenExplicitlyListed() {
        System.setProperty(HeaderPropagationConfiguration.HEADERS_BLOCKED_PROPERTY, "X-Channel-Request-Id, X-Request-Id");
        HeaderPropagationConfiguration.resetCache();
    
        // X-Request-Id is never blocked
        Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Request-Id"));
        // X-Channel-Request-Id blocked
        Assertions.assertTrue(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"));
        // there is no X-Request-Id in a list
        Assertions.assertFalse(HeaderPropagationConfiguration.blockedHeaders()
                .stream().anyMatch(h -> h.equalsIgnoreCase("X-Request-Id")));
    }
    
    @Test
    void shouldReturnEmptyListWhenEnvExplicitlyEmpty() throws Exception {
        environmentVariables.set(HeaderPropagationConfiguration.HEADERS_BLOCKED_ENV, "");
        try {
            HeaderPropagationConfiguration.resetCache();
            Assertions.assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"));
            Assertions.assertTrue(HeaderPropagationConfiguration.blockedHeaders().isEmpty());
        } finally {
            environmentVariables.remove(HeaderPropagationConfiguration.HEADERS_BLOCKED_ENV);
            HeaderPropagationConfiguration.resetCache();
        }
    }
    
    @Test
    void shouldBlockedHeadersReturnCorrectList() {
        System.setProperty(HeaderPropagationConfiguration.HEADERS_BLOCKED_PROPERTY, "Custom-Header, X-Request-Id");
        HeaderPropagationConfiguration.resetCache();
    
        List<String> blocked = HeaderPropagationConfiguration.blockedHeaders();
        Assertions.assertTrue(blocked.contains("Custom-Header"));
        Assertions.assertFalse(blocked.stream().anyMatch(h -> h.equalsIgnoreCase("X-Request-Id")));
    }
}
