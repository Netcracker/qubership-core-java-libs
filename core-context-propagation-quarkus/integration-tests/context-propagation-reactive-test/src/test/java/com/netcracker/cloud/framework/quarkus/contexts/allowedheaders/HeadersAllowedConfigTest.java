package com.netcracker.cloud.framework.quarkus.contexts.allowedheaders;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class HeadersAllowedConfigTest {

    @Inject
    HeadersAllowedConfig headersAllowedConfig;

    @Test
    void shouldReadHeadersAllowedFromProperty() {
        Optional<String> value = headersAllowedConfig.allowedHeaders();

        assertTrue(value.isPresent(), "quarkus.headers.allowed must be present");
        assertEquals("test-quarkus.headers.allowed", value.get());
    }

    @Test
    void shouldReadHeadersBlockedAsExplicitlyEmpty() {
        assertTrue(headersAllowedConfig.isBlockedHeadersSet(),
                "quarkus.headers.blocked must be considered explicitly set");
        assertEquals("", headersAllowedConfig.blockedHeaders(),
                "quarkus.headers.blocked must be preserved as an empty string");
    }
}
