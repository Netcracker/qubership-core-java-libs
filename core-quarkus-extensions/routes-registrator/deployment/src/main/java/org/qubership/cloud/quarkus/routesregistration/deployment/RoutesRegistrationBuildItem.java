package org.qubership.cloud.quarkus.routesregistration.deployment;

import org.qubership.cloud.routesregistration.common.gateway.route.RouteEntry;
import io.quarkus.builder.item.SimpleBuildItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class RoutesRegistrationBuildItem extends SimpleBuildItem {
    private final Collection<RouteEntry> routes;

    public RoutesRegistrationBuildItem(Collection<RouteEntry> routes) {
        this.routes = routes;
    }

    /**
     * {@code List<RouteEntry>}-based API is kept for backward compatibility.
     * Please, prefer {@code Collection<RouteEntry>}-based API for better performance.
     *
     * @return RouteEntries set transformed to {@code List}.
     */
    @Deprecated
    public List<RouteEntry> getRoutes() {
        return routes instanceof List ? (List<RouteEntry>) routes : new ArrayList<>(routes);
    }

    public Collection<RouteEntry> getRouteEntries() {
        return routes;
    }
}
