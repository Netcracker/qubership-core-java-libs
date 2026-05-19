package com.netcracker.cloud.framework.quarkus.contexts.xchannelrequestid;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;


import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Integration scenario: {@code quarkus.context.propagation.headers.enable.optional} is effectively
 * not configured. Verifies that {@link HeadersOptionalConfig#enableOptional()} resolves to
 * {@link java.util.Optional#empty()} — which is the signal the recorder uses to leave the
 * system property untouched and let the restricted list apply in full.
 *
 * <p>The module's {@code application.properties} sets the property to {@code X-Channel-Request-Id},
 * so we use a {@link QuarkusTestProfile} that overrides it to an empty value. Under SmallRye Config
 * semantics, an empty value for {@code Optional<List<String>>} resolves to {@code Optional.empty()} —
 * exactly the same observable state as "not configured".</p>
 */
@QuarkusTest
@TestProfile(HeadersOptionalConfigNotSetTest.NotSetProfile.class)
class HeadersOptionalConfigNotSetTest {

    @Inject
    HeadersOptionalConfig headersOptionalConfig;

    @Test
    void shouldResolveToEmptyWhenNotConfigured() {
        assertFalse(headersOptionalConfig.enableOptional().isPresent(),
                "quarkus.context.propagation.headers.enable.optional must resolve to Optional.empty() " +
                        "when no value is configured");
    }

    public static class NotSetProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.context.propagation.headers.enable.optional", "");
        }
    }
}
