package com.netcracker.cloud.framework.quarkus.contexts.allowedheaders;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void shouldReadAllowedHeadersFromBlocklist() {
        Optional<List<String>> value = headersAllowedConfig.allowedHeadersFromBlocklist();

        assertTrue(value.isPresent(),
                "quarkus.context.propagation.allow-blocked-headers must be present when configured");
        assertEquals(List.of("X-Channel-Request-Id"), value.get(),
                "SmallRye must parse the comma-separated value into a list");
    }
}
