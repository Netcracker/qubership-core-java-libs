package com.netcracker.cloud.routesregistration.common.gateway.route;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GatewayNameKeyTest {
    @Test
    void fromGatewayName() {
        assertEquals(GatewayNameKey.PUBLIC, GatewayNameKey.fromGatewayName("public-gateway-service"));
        assertEquals(GatewayNameKey.PRIVATE, GatewayNameKey.fromGatewayName("private-gateway-service"));
        assertEquals(GatewayNameKey.INTERNAL, GatewayNameKey.fromGatewayName("internal-gateway-service"));
        assertEquals(GatewayNameKey.FACADE, GatewayNameKey.fromGatewayName("some-facade-gateway-service"));
        assertEquals(GatewayNameKey.INTERNAL, GatewayNameKey.fromGatewayName(null));
        assertEquals(GatewayNameKey.INTERNAL, GatewayNameKey.fromGatewayName(""));
    }

    @Test
    void toGatewayName() {
        assertEquals("public-gateway-service", GatewayNameKey.PUBLIC.toGatewayName());
        assertEquals("private-gateway-service", GatewayNameKey.PRIVATE.toGatewayName());
        assertEquals("internal-gateway-service", GatewayNameKey.INTERNAL.toGatewayName());
        assertNull(GatewayNameKey.FACADE.toGatewayName());
    }
}
