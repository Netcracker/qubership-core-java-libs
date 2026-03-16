package com.netcracker.cloud.routesregistration.common.gateway.route;

import static com.netcracker.cloud.routesregistration.common.gateway.route.Constants.*;

public enum GatewayNameKey {
    PUBLIC,
    PRIVATE,
    INTERNAL,
    FACADE;

    public static GatewayNameKey fromGatewayName(String gateway) {
        if (gateway == null || gateway.isEmpty()) {
            // default route type is INTERNAL
            return INTERNAL;
        }
        return switch (gateway) {
            case PUBLIC_GATEWAY_SERVICE -> PUBLIC;
            case PRIVATE_GATEWAY_SERVICE -> PRIVATE;
            case INTERNAL_GATEWAY_SERVICE -> INTERNAL;
            default -> FACADE;
        };
    }

    public String toGatewayName() {
        return switch (this) {
            case PUBLIC -> PUBLIC_GATEWAY_SERVICE;
            case PRIVATE -> PRIVATE_GATEWAY_SERVICE;
            case INTERNAL -> INTERNAL_GATEWAY_SERVICE;
            default -> null;
        };
    }
}
