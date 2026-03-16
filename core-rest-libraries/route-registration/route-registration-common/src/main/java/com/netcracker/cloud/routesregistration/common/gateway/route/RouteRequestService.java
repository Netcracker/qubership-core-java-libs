package com.netcracker.cloud.routesregistration.common.gateway.route;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 The class provides the conversion of RouteEntry to RequestEntity of various versions of the Control-Plane API.
 The microserviceURL parameter in all methods is required to maintain backward compatibility with RoutesRestRegistrationProcessor
 */
@Deprecated
public interface RouteRequestService {
    Map<GatewayNameKey, List<?>> getRegistrationRequestsPerGateway(String microserviceURL, Collection<RouteEntry> routes);
}
