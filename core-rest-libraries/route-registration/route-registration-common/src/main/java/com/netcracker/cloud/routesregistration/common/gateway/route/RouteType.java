package com.netcracker.cloud.routesregistration.common.gateway.route;


import static com.netcracker.cloud.routesregistration.common.gateway.route.Constants.*;

/**
 * RouteType specifies in which gateways this route should be registered.
 * <ul>
 *  <li>
 *      Facade gateway serves particular family (group of different versions of the same microservice) in Blue-Green routing model.
 *  </li>
 *  <li>
 *      Internal (back-office) gateway manages API routing necessary for internal backend-microservice communications.
 *      It knows about all microservice routes.
 *  </li>
 *  <li>
 *      Public gateway manages requests coming from frontend-microservices and provides access to publicly available APIs (public routes).
 *  </li>
 *  <li>
 *      Private gateway manages requests coming from frontend-microservices and provides access to publicly available APIs
 *      and APIs with a limited access, available to privileged users (private routes)
 *  </li>
 * </ul>
 */
public enum RouteType {
    /**
     * Facade route type: indicates that it should be sent to facade gateway
     */
    FACADE,

    /**
     * Internal route type: indicates that it should be sent to internal gateway
     */
    INTERNAL,

    /**
     * Private route type: indicates that it should be sent to internal &amp; private gateways
     */
    PRIVATE,

    /**
     * Public route type: indicates that it should be sent to internal, private &amp; public gateways
     */
    PUBLIC;

    public static RouteType fromGatewayName(String gateway) {
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
