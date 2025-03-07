package org.qubership.cloud.routesregistration.common.annotation.processing;

import org.qubership.cloud.routesregistration.common.annotation.FacadeRoute;
import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteAnnotationInfo {
    private RouteType routeType;
    private Set<String> gateways;
    private Set<String> hosts;
    private Long timeout;

    public RouteAnnotationInfo(Route route) {
        this.routeType = RouteAnnotationUtils.getAnnotationRouteType(route);

        String[] gateways = RouteAnnotationUtils.getAnnotationGateways(route);
        if (gateways != null) {
            this.gateways = new HashSet<>(gateways.length);
            Collections.addAll(this.gateways, gateways);
        }

        this.timeout = RouteAnnotationUtils.getAnnotationRouteTimeout(route);
    }

    public RouteAnnotationInfo(FacadeRoute route) {
        this.routeType = RouteType.FACADE;

        String[] gateways = RouteAnnotationUtils.getAnnotationGateways(route);
        if (gateways != null) {
            this.gateways = new HashSet<>(gateways.length);
            Collections.addAll(this.gateways, gateways);
        }

        this.timeout = RouteAnnotationUtils.getAnnotationRouteTimeout(route);
    }
}