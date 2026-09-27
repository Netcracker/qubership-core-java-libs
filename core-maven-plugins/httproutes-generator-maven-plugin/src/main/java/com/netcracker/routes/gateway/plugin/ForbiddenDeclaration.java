package com.netcracker.routes.gateway.plugin;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * A gateway path forbidden by {@code @ForbiddenRoute} on the listed border gateways.
 *
 * @param origin {@code class#method} or {@code class} that carries the annotation
 */
public record ForbiddenDeclaration(String gatewayPath, Set<Gateway> gateways, String origin) {

    public ForbiddenDeclaration {
        gatewayPath = PathPattern.normalize(gatewayPath);
        gateways = Collections.unmodifiableSet(gateways.isEmpty() ? EnumSet.noneOf(Gateway.class) : EnumSet.copyOf(gateways));
    }
}
