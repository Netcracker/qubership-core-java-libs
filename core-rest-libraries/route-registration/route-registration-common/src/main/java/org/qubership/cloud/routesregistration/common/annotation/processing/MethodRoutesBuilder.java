package org.qubership.cloud.routesregistration.common.annotation.processing;

import java.util.List;
import java.util.Set;

public class MethodRoutesBuilder extends AbstractRoutesBuilder<MethodRoutesBuilder, MethodRoutesBuilder> {

    @Deprecated
    public MethodRoutesBuilder() {
        this.microserviceName = null;
        this.routeHostMapping = new RouteHostMapping();
    }

    public MethodRoutesBuilder(String microserviceName, RouteHostMapping routeHostMapping) {
        this.microserviceName = microserviceName;
        this.routeHostMapping = routeHostMapping;
    }

    protected List<RouteAnnotationInfo> getRoutes() {
        return routes;
    }

    protected RouteAnnotationInfo getFacadeRoute () {
        return facadeRoute;
    }

    protected Set<String> getPathsTo() {
        return pathsTo;
    }

    protected Set<String> getGatewayPathsFrom () {
        return gatewayPathsFrom;
    }

    protected Set<String> getFacadeGatewayPathsFrom() {
        return facadeGatewayPathsFrom;
    }

    /**
     * Simply returns this {@code MethodRoutesBuilder} instance without any additional logic,
     * because this builder implementation is only used by {@link ClassRoutesBuilder}
     * and does not build something useful itself.
     *
     * @return this
     */
    @Override
    public MethodRoutesBuilder build() {
        return this;
    }

    @Override
    protected MethodRoutesBuilder getThis() {
        return this;
    }
}
