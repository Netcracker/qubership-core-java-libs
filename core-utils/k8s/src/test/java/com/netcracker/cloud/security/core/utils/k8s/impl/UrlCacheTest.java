package com.netcracker.cloud.security.core.utils.k8s.impl;

import org.junit.jupiter.api.Test;

import static com.netcracker.cloud.security.core.utils.k8s.impl.UrlCache.calculateCacheKey;
import static org.junit.jupiter.api.Assertions.*;

class UrlCacheTest {

    @Test
    void calculateCacheKeyTest() {
        String key = calculateCacheKey("https://internal-gateway-service:3030/api/v1/service-a/resource/123");
        assertEquals("internal-gateway-service:3030/api/v1/service-a", key);

        key = calculateCacheKey("https://internal-gateway-service:3030/api/v1");
        assertEquals("internal-gateway-service:3030/api/v1", key);

        key = calculateCacheKey("https://internal-gateway-service:3030/custom-prefix/api/v2/module-b/action");
        assertEquals("internal-gateway-service:3030/custom-prefix/api/v2", key);

        key = calculateCacheKey("https://internal-gateway-service:3030/long/complex/path/v3/target/item");
        assertEquals("internal-gateway-service:3030/long/complex/path/v3", key);

        key = calculateCacheKey("https://internal-gateway-service:3030/api/v/resource");
        assertEquals("internal-gateway-service:3030/api/v/resource", key);

        key = calculateCacheKey("https://internal-gateway-service:3030/api/vv/resource");
        assertEquals("internal-gateway-service:3030/api/vv/resource", key);

        key = calculateCacheKey("https://internal-gateway-service:3030/api/v1/service?query=param&data=true");
        assertEquals("internal-gateway-service:3030/api/v1/service", key);

        key = calculateCacheKey("https://external-service:8080/api/v1/resource");
        assertEquals("external-service:8080", key);

        assertThrows(IllegalArgumentException.class, () -> calculateCacheKey("illegal characters here"));
    }
}
