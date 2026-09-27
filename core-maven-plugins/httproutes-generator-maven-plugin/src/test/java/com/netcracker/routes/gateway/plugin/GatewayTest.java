package com.netcracker.routes.gateway.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayTest {

    @Test
    void exposureChainFollowsRouteType() {
        assertEquals(Set.of(Gateway.PUBLIC, Gateway.PRIVATE, Gateway.INTERNAL), Gateway.exposedBy(HttpRoute.Type.PUBLIC));
        assertEquals(Set.of(Gateway.PRIVATE, Gateway.INTERNAL), Gateway.exposedBy(HttpRoute.Type.PRIVATE));
        assertEquals(Set.of(Gateway.INTERNAL), Gateway.exposedBy(HttpRoute.Type.INTERNAL));
        assertEquals(Set.of(), Gateway.exposedBy(HttpRoute.Type.FACADE));
    }

    @Test
    void lookupByLegacyGatewayName() {
        assertEquals(Optional.of(Gateway.PUBLIC), Gateway.fromLegacyName("public-gateway-service"));
        assertEquals(Optional.of(Gateway.PRIVATE), Gateway.fromLegacyName("private-gateway-service"));
        assertEquals(Optional.of(Gateway.INTERNAL), Gateway.fromLegacyName("internal-gateway-service"));
        assertEquals(Optional.empty(), Gateway.fromLegacyName("composite-gw"));
        assertEquals(Optional.empty(), Gateway.fromLegacyName("public-gateway"));
        assertEquals(Optional.empty(), Gateway.fromLegacyName(""));
        assertEquals(Optional.empty(), Gateway.fromLegacyName(null));
    }

    @Test
    void lookupByRouteTypeName() {
        assertEquals(Optional.of(Gateway.PUBLIC), Gateway.fromRouteTypeName("PUBLIC"));
        assertEquals(Optional.of(Gateway.INTERNAL), Gateway.fromRouteTypeName("INTERNAL"));
        assertEquals(Optional.empty(), Gateway.fromRouteTypeName("FACADE"));
    }

    @Test
    void istioReferenceData() {
        assertEquals("gateway.networking.k8s.io", Gateway.PUBLIC.refGroup());
        assertEquals("Gateway", Gateway.PUBLIC.refKind());
        assertEquals("public-gateway", Gateway.PUBLIC.refName());
        assertEquals("Gateway", Gateway.PRIVATE.refKind());
        assertEquals("private-gateway", Gateway.PRIVATE.refName());
        assertEquals("", Gateway.INTERNAL.refGroup());
        assertEquals("Service", Gateway.INTERNAL.refKind());
        assertEquals("internal-gateway-service", Gateway.INTERNAL.refName());
    }

    @Test
    void widestType() {
        assertEquals(HttpRoute.Type.PUBLIC, Gateway.widest(List.of(HttpRoute.Type.INTERNAL, HttpRoute.Type.PUBLIC, HttpRoute.Type.PRIVATE)));
        assertEquals(HttpRoute.Type.PRIVATE, Gateway.widest(List.of(HttpRoute.Type.INTERNAL, HttpRoute.Type.PRIVATE)));
    }
}
