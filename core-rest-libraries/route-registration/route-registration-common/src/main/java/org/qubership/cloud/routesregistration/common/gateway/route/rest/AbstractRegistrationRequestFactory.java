package org.qubership.cloud.routesregistration.common.gateway.route.rest;

import org.qubership.cloud.routesregistration.common.gateway.route.RouteEntry;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteType;
import lombok.RequiredArgsConstructor;

import java.util.Collection;

@RequiredArgsConstructor
public abstract class AbstractRegistrationRequestFactory {
    protected final String microserviceURL;
    protected final String microserviceName;
    protected final String deploymentVersion;
    protected final String cloudNamespace;

    public abstract CompositeRequest<CommonRequest> createRequests(Collection<RouteEntry> routes);

    protected String resolveGatewayName(RouteEntry routeEntry) {
        if (routeEntry.getGateway() != null && !routeEntry.getGateway().isEmpty()) {
            return routeEntry.getGateway();
        }
        if (routeEntry.getType() == RouteType.FACADE) {
            return microserviceName;
        }
        return routeEntry.getType().toGatewayName();
    }
}
