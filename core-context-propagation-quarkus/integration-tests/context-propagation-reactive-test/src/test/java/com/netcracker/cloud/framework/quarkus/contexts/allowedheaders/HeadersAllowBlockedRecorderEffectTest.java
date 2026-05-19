package com.netcracker.cloud.framework.quarkus.contexts.allowedheaders;

import com.netcracker.cloud.framework.contexts.allowedheaders.HeaderPropagationConfiguration;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class HeadersAllowBlockedRecorderEffectTest {

    @Test
    void shouldExposeAllowBlockedValueAsSystemProperty() {
        assertEquals("X-Channel-Request-Id",
                System.getProperty("context.propagation.allow-blocked-headers"),
                "Recorder must propagate the quarkus.context.propagation.allow-blocked-headers value " +
                        "to the context.propagation.allow-blocked-headers system property.");
    }

    @Test
    void shouldRemoveExemptedHeaderFromInternalBlocklist() {
        // The blocked list is cached on first access; ensure we read the post-recorder state.
        HeaderPropagationConfiguration.resetCache();

        assertTrue(HeaderPropagationConfiguration.blockedHeaders().isEmpty(),
                "The only entry of the internal blocklist (X-Channel-Request-Id) must be removed " +
                        "by the exemption configured in application.properties.");
        assertFalse(HeaderPropagationConfiguration.isBlacklisted("X-Channel-Request-Id"),
                "X-Channel-Request-Id must not be blocked when explicitly exempted.");
    }
}
