package org.qubership.cloud.quarkus.routesregistration.deployment.resource;

import org.qubership.cloud.routesregistration.common.annotation.FacadeGateway;
import org.qubership.cloud.routesregistration.common.annotation.FacadeRoute;
import org.qubership.cloud.routesregistration.common.annotation.Gateway;
import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Route(RouteType.PUBLIC)
@FacadeRoute
@Path("/custom-route-test")
@Gateway("/gateway-custom-route-test")
@FacadeGateway("/custom-route-test")
public class CustomerResourceWithCustomRoute {
    @GET
    @Path("/public")
    @Produces(MediaType.APPLICATION_JSON)
    public int publicRoute() {
        return 0;
    }

    @GET
    @Path("/private")
    @Route(value = RouteType.PRIVATE)
    @Gateway("/gateway-private")
    @Produces(MediaType.APPLICATION_JSON)
    public int privateRoute() {
        return 0;
    }

    @GET
    @Path("/facade")
    @FacadeRoute
    @FacadeGateway("/facade-route")
    @Produces(MediaType.APPLICATION_JSON)
    public int facadeRoute() {
        return 0;
    }
}
