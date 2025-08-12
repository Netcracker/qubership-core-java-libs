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
        if (PUBLIC_GATEWAY_SERVICE.equals(gateway)) {
            return PUBLIC;
        }
        if (PRIVATE_GATEWAY_SERVICE.equals(gateway)) {
            return PRIVATE;
        }
        if (INTERNAL_GATEWAY_SERVICE.equals(gateway)) {
            return INTERNAL;
        }
        return FACADE;
    }

    public String toGatewayName() {
        switch (this) {
            case PUBLIC:
                return PUBLIC_GATEWAY_SERVICE;
            case PRIVATE:
                return PRIVATE_GATEWAY_SERVICE;
            case INTERNAL:
                return INTERNAL_GATEWAY_SERVICE;
            default:
                return null;
        }
    }
}
