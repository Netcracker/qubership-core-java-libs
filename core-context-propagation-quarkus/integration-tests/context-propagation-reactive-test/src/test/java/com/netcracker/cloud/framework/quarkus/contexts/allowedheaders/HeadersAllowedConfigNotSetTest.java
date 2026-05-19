package com.netcracker.cloud.framework.quarkus.contexts.allowedheaders;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
@TestProfile(HeadersAllowedConfigNotSetTest.NotSetProfile.class)
class HeadersAllowedConfigNotSetTest {

    @Inject
    HeadersAllowedConfig headersAllowedConfig;

    @Test
    void shouldReportAllowedFromBlocklistAsEmptyWhenNotConfigured() {
        assertFalse(headersAllowedConfig.allowedHeadersFromBlocklist().isPresent(),
                "quarkus.context.propagation.allow-blocked-headers must resolve to Optional.empty() " +
                        "when no exemption value is configured");
    }

    public static class NotSetProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.context.propagation.allow-blocked-headers", "");
        }
    }
}
