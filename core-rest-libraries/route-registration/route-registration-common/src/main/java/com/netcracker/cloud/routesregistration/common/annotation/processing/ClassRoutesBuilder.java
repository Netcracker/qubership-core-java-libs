package com.netcracker.cloud.routesregistration.common.annotation.processing;

import com.netcracker.cloud.routesregistration.common.gateway.route.RouteEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Class to be used for creating routes based on all the class-level and method-level annotations of the controller class.</p>
 *
 * <p>{@link ClassRoutesBuilder#withMethod(MethodRoutesBuilder)} is used to provide information obtained from method-level annotations.
 * All the class-level annotations info should be provided by other methods of this {@code ClassRoutesBuilder} instance.</p>
 *
 * <p>Builder with filled fields or the result of {@link ClassRoutesBuilder#build} should be provided to
 * {@link MicroserviceRoutesBuilder}, so routes can be validated before registration.</p>
 */
public class ClassRoutesBuilder extends AbstractRoutesBuilder<ClassRoutesBuilder, List<RouteEntry>> {
    @Deprecated
    public ClassRoutesBuilder(String microserviceName) {
        this.microserviceName = microserviceName;
        this.routeHostMapping = new RouteHostMapping();
    }

    public ClassRoutesBuilder(String microserviceName, RouteHostMapping routeHostMapping) {
        this.microserviceName = microserviceName;
        this.routeHostMapping = routeHostMapping;
    }

    private final List<MethodRoutesBuilder> methods = new ArrayList<>();

    public ClassRoutesBuilder withMethod(MethodRoutesBuilder methodRoutesBuilder) {
        this.methods.add(methodRoutesBuilder);
        return this;
    }

    /**
     * <p>Builds list of routes from the provided information about class and methods.</p>
     *
     * <p>This list can contain duplicates and routes which collide with other microservice routes,
     * so this result should be passed to {@link MicroserviceRoutesBuilder} before usage.</p>
     *
     * @return list of routes build by this class annotations, without any validations.
     */
    @Override
    public List<RouteEntry> build() {
        if (microserviceName == null || microserviceName.isEmpty()) {
            throw new IllegalStateException("microserviceName must not be empty");
        }

        ClassRoutesSet classRoutesSet = new ClassRoutesSet(
                microserviceName,
                pathsTo,
                gatewayPathsFrom,
                facadeGatewayPathsFrom,
                routes,
                facadeRoute);

        methods.forEach(method -> classRoutesSet.addMethodInfo(
                method.getPathsTo(),
                method.getRoutes(),
                method.getFacadeRoute(),
                method.getGatewayPathsFrom(),
                method.getFacadeGatewayPathsFrom()));

        return classRoutesSet.getActualRoutes();
    }

    @Override
    protected ClassRoutesBuilder getThis() {
        return this;
    }
}
