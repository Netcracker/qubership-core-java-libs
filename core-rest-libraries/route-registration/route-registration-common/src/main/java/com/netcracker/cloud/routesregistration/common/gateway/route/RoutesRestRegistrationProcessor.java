package com.netcracker.cloud.routesregistration.common.gateway.route;

import com.netcracker.cloud.routesregistration.common.gateway.route.rest.CommonRequest;
import com.netcracker.cloud.routesregistration.common.gateway.route.rest.CompositeRequest;
import com.netcracker.cloud.routesregistration.common.gateway.route.rest.ControlPlaneApiVersion;
import com.netcracker.cloud.routesregistration.common.gateway.route.rest.RegistrationRequestFactory;
import com.netcracker.cloud.routesregistration.common.gateway.route.transformation.RouteTransformer;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class RoutesRestRegistrationProcessor {

    private final ControlPlaneClient controlPlaneClient;
    private final RouteRetryManager routeRetryManager;
    private final Boolean postRoutesEnabled;
    private final String microserviceInternalUrl;

    private String microserviceName;
    private RouteTransformer routeTransformer;
    private RegistrationRequestFactory registrationRequestFactory;

    /**
     * This constructor is kept only for backward compatibility.
     * New clients should use {@link RoutesRestRegistrationProcessor#RoutesRestRegistrationProcessor(ControlPlaneClient controlPlaneClient,
     * RouteRetryManager routeRetryManager,
     * RouteTransformer routeTransformer,
     * RegistrationRequestFactory registrationRequestFactory,
     * Boolean postRoutesEnabled,
     * String microserviceName,
     * String microserviceInternalUrl)}.
     *
     * @param controlPlaneClient      {@link ControlPlaneClient} implementation.
     * @param routeRetryManager       retry manager for route registration requests.
     * @param postRoutesEnabled       switch to enable routes registration.
     * @param microserviceInternalUrl cloud internal URL to microservice that is used in routes' {@code "endpoint"} field.
     */
    public RoutesRestRegistrationProcessor(ControlPlaneClient controlPlaneClient,
                                           RouteRetryManager routeRetryManager,
                                           RouteTransformer routeTransformer,
                                           RegistrationRequestFactory registrationRequestFactory,
                                           Boolean postRoutesEnabled,
                                           String microserviceName,
                                           String microserviceInternalUrl) {
        this.controlPlaneClient = controlPlaneClient;
        this.routeRetryManager = routeRetryManager;
        this.routeTransformer = routeTransformer;
        this.registrationRequestFactory = registrationRequestFactory;
        this.postRoutesEnabled = postRoutesEnabled;
        this.microserviceName = microserviceName;
        this.microserviceInternalUrl = microserviceInternalUrl;
    }

    public void postRoutes(final Collection<RouteEntry> routes) {
        postRoutes(microserviceInternalUrl, routes);
    }

    public void postRoutes(final String microserviceUrl, Collection<RouteEntry> routes) {
        if (Boolean.FALSE.equals(postRoutesEnabled)) {
            log.info("Skip posting routes to gateway: {}",
                    routes.stream().map(RouteEntry::toString).collect(Collectors.joining(", ")));
            return;
        }
        if (routes.isEmpty()) {
            return;
        }
        performRoutesRegistration(microserviceUrl, routes);
    }

    private void performRoutesRegistration(String microserviceUrl, Collection<RouteEntry> routes) {
        Collection<RouteEntry> transformedRoutes = routeTransformer.transform(routes);
        CompositeRequest<CommonRequest> requests = registrationRequestFactory.createRequests(
                microserviceUrl,
                transformedRoutes,
                ControlPlaneApiVersion.V3);

        Map<Integer, List<Runnable>> priorityPayload = new HashMap<>();
        requests.forEach(request -> priorityPayload.computeIfAbsent(request.getPriority(), any -> new ArrayList<>())
                .add(() -> controlPlaneClient.sendRequest(request)));

        routeRetryManager.execute(priorityPayload);
    }
}
