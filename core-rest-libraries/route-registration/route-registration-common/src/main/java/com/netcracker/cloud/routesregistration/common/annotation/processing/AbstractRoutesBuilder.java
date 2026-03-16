package com.netcracker.cloud.routesregistration.common.annotation.processing;

import com.netcracker.cloud.routesregistration.common.annotation.*;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import com.netcracker.cloud.routesregistration.common.gateway.route.Utils;

import java.util.*;

import static com.netcracker.cloud.routesregistration.common.annotation.processing.RouteAnnotationUtils.isSingleEmptyStringArray;

/**
 * Abstract class for builders that build routes by information from annotations.
 * Can be used both for class-level annotation and method-level annotation processing.
 *
 * @param <T> type of builder itself (implementation class type),
 *            used to return {@code this} when setting builder fields.
 * @param <R> builder resulting type returned by the {@code build()} method.
 */
public abstract class AbstractRoutesBuilder<T extends AbstractRoutesBuilder<T, R>, R> {
    protected String microserviceName;
    protected RouteHostMapping routeHostMapping;
    protected List<RouteAnnotationInfo> routes;
    @Deprecated
    protected RouteAnnotationInfo facadeRoute;

    protected Set<String> pathsTo;
    protected Set<String> gatewayPathsFrom;
    protected Set<String> facadeGatewayPathsFrom;

    public abstract R build();

    /**
     * Method that returns {@code this} of type T, which is a child builder type.
     *
     * @return {@code this} of type T.
     */
    protected abstract T getThis();

    public T withRouteAnnotation(Routes routes) {
        if (routes != null) {
            for (Route route : routes.value()) {
                this.withRouteAnnotation(route);
            }
        }
        return getThis();
    }

    public T withRouteAnnotation(Route route) {
        if (route == null) {
            return getThis();
        }
        RouteAnnotationInfo routeInfo = RouteAnnotationInfo.builder()
                .routeType(RouteAnnotationUtils.getAnnotationRouteType(route))
                .timeout(RouteAnnotationUtils.getAnnotationRouteTimeout(route))
                .gateways(getGateways(route))
                .hosts(getHosts(route))
                .build();
        return withRouteAnnotation(routeInfo);
    }

    public T withRouteAnnotation(RouteAnnotationInfo route) {
        if (route == null) {
            return getThis();
        }
        addRoute(route);
        return getThis();
    }

    private void addRoute(RouteAnnotationInfo route) {
        if (Utils.isEmpty(this.routes)) {
            this.routes = new ArrayList<>();
        }
        if (routeHostMapping != null && !routeHostMapping.isEmpty()) {
            propagateDefaultGateway(route);
            propagateDefaultHosts(route);
        }
        validateRouteHosts(route);
        this.routes.add(route);
    }

    @Deprecated
    public T withFacadeRouteAnnotation(FacadeRoute facadeRoute) {
        this.facadeRoute = facadeRoute == null ? null : new RouteAnnotationInfo(facadeRoute);
        return getThis();
    }

    @Deprecated
    public T withFacadeRouteAnnotation(RouteAnnotationInfo facadeRoute) {
        this.facadeRoute = facadeRoute;
        return getThis();
    }

    public T withPathsTo(Set<String> paths) {
        this.pathsTo = paths;
        return getThis();
    }

    public T withPathsTo(String[] paths) {
        if (isSingleEmptyStringArray(paths)) {
            this.pathsTo = null;
        } else {
            this.pathsTo = new HashSet<>(paths.length);
            Collections.addAll(this.pathsTo, paths);
        }
        return getThis();
    }

    public T withGatewayAnnotation(Gateway gateway) {
        return gateway == null ? getThis() : withGatewayPathsFrom(gateway.value());
    }

    @Deprecated
    public T withFacadeGatewayAnnotation(FacadeGateway facadeGateway) {
        return facadeGateway == null ? getThis() : withFacadeGatewayPathsFrom(facadeGateway.value());
    }

    public T withGatewayPathsFrom(Set<String> paths) {
        this.gatewayPathsFrom = paths;
        return getThis();
    }

    public T withGatewayPathsFrom(String[] paths) {
        if (isSingleEmptyStringArray(paths)) {
            this.gatewayPathsFrom = null;
        } else {
            this.gatewayPathsFrom = new HashSet<>(paths.length);
            Collections.addAll(this.gatewayPathsFrom, paths);
        }
        return getThis();
    }

    public T withFacadeGatewayPathsFrom(Set<String> paths) {
        this.facadeGatewayPathsFrom = paths;
        return getThis();
    }

    public T withFacadeGatewayPathsFrom(String[] paths) {
        if (isSingleEmptyStringArray(paths)) {
            this.facadeGatewayPathsFrom = null;
        } else {
            this.facadeGatewayPathsFrom = new HashSet<>(paths.length);
            Collections.addAll(this.facadeGatewayPathsFrom, paths);
        }
        return getThis();
    }

    private void validateRouteHosts(RouteAnnotationInfo route) {
        if (Utils.isNotEmpty(route.getHosts()) && (isContainsForbiddenGateway(route.getGateways()))) {
            throw new IllegalArgumentException("Only composite gateway can have hosts");
        }
    }

    private boolean isContainsForbiddenGateway(Set<String> gateways) {
        if (Utils.isEmpty(gateways)) {
            return false;
        }
        for (String gateway : gateways) {
            if (Utils.isBorderGatewayName(gateway) || microserviceName == null || gateway.equalsIgnoreCase(microserviceName)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> getGateways(Route route) {
        String[] routeGateways = RouteAnnotationUtils.getAnnotationGateways(route);
        Set<String> resultGateways = null;
        if (routeGateways != null) {
            resultGateways = new HashSet<>(routeGateways.length);
            Collections.addAll(resultGateways, routeGateways);
        }

        return resultGateways;
    }

    private Set<String> getHosts(Route route) {
        Set<String> resultHosts = null;
        String[] hosts = RouteAnnotationUtils.getAnnotationHosts(route);
        if (hosts != null) {
            resultHosts = new HashSet<>(hosts.length);
            Collections.addAll(resultHosts, hosts);
        }
        return resultHosts;
    }

    private void propagateDefaultGateway(RouteAnnotationInfo route) {
        Set<String> gateways = route.getGateways();
        if (Utils.isNotEmpty(gateways) && gateways.stream().anyMatch(Utils::isBorderGatewayName)) {
            return;
        }
        if (Utils.isEmpty(gateways) && route.getRouteType() == RouteType.FACADE) {
            Set<String> resultGateways = new HashSet<>(1);
            resultGateways.add(routeHostMapping.getGatewayName());
            route.setGateways(resultGateways);
        }
    }

    private void propagateDefaultHosts(RouteAnnotationInfo route) {
        if (shouldAddDefaultHost(route)) {
            List<String> defaultHosts = routeHostMapping.getVirtualHosts();
            Set<String> resultHosts = new HashSet<>(defaultHosts.size());
            resultHosts.addAll(defaultHosts);
            route.setHosts(resultHosts);
        }
    }

    private boolean shouldAddDefaultHost(RouteAnnotationInfo route) {
        return Utils.isEmpty(route.getHosts()) && Utils.isNotEmpty(route.getGateways()) && route.getGateways().contains(routeHostMapping.getGatewayName());
    }
}
