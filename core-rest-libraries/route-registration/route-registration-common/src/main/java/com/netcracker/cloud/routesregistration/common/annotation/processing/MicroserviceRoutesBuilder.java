package com.netcracker.cloud.routesregistration.common.annotation.processing;

import com.netcracker.cloud.routesregistration.common.gateway.route.RouteEntry;

import java.util.*;

/**
 * Class to collect all the microservice routes by annotation info before registration.
 */
public class MicroserviceRoutesBuilder {
    private final List<RouteEntry> routes = new ArrayList<>();

    public MicroserviceRoutesBuilder withClass(ClassRoutesBuilder classRoutesBuilder) {
        this.withRoutes(classRoutesBuilder.build());
        return this;
    }

    public MicroserviceRoutesBuilder withRoutes(RouteEntry ...routes) {
        Collections.addAll(this.routes, routes);
        return this;
    }

    public MicroserviceRoutesBuilder withRoutes(Collection<RouteEntry> routes) {
        this.routes.addAll(routes);
        return this;
    }

    public Collection<RouteEntry> build() {
        return routes;
    }
}
