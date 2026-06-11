package com.netcracker.routes.gateway.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpRouteTest {

    @Test
    void normalizesPathsWithLeadingSlash() {
        HttpRoute route = new HttpRoute("api/v1", HttpRoute.Type.PUBLIC, 0);

        assertEquals("/api/v1", route.path());
        assertEquals("/api/v1", route.gatewayPath());
    }

    @Test
    void normalizesCustomGatewayPath() {
        HttpRoute route = new HttpRoute("/service", "gateway", HttpRoute.Type.PRIVATE);

        assertEquals("/service", route.path());
        assertEquals("/gateway", route.gatewayPath());
    }

    @Test
    void defaultsNullPathsToRoot() {
        HttpRoute route = new HttpRoute(null, HttpRoute.Type.INTERNAL);

        assertEquals("/", route.path());
        assertEquals("/", route.gatewayPath());
    }

    @Test
    void rejectsNullType() {
        assertThrows(NullPointerException.class, () -> new HttpRoute("/api", null));
    }
}
