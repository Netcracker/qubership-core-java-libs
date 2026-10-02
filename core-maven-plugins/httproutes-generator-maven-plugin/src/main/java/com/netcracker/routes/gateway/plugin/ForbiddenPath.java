package com.netcracker.routes.gateway.plugin;

import java.util.Set;

/**
 * A gateway path that {@code @ForbiddenRoute} forbids on the given border gateways.
 */
public record ForbiddenPath(String gatewayPath, Set<HttpRoute.Type> gateways) {

    public ForbiddenPath {
        gatewayPath = gatewayPath.startsWith("/") ? gatewayPath : "/" + gatewayPath;
        gateways = Set.copyOf(gateways);
    }
}
