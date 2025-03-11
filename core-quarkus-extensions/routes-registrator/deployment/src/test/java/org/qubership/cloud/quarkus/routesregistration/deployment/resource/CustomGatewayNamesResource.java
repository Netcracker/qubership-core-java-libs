package org.qubership.cloud.quarkus.routesregistration.deployment.resource;

import org.qubership.cloud.routesregistration.common.annotation.FacadeGateway;
import org.qubership.cloud.routesregistration.common.annotation.FacadeRoute;
import org.qubership.cloud.routesregistration.common.annotation.Gateway;
import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.qubership.cloud.routesregistration.common.gateway.route.Constants;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Route(gateways = Constants.INTERNAL_GATEWAY_SERVICE)
@Gateway("/api/v1/test-ms/test-controller")
@FacadeGateway("/api/v1/test-controller")
@Path("/test-controller")
public class CustomGatewayNamesResource {
    @GET
    @Path("/first")
    @Produces(MediaType.APPLICATION_JSON)
    public int first() {
        return 0;
    }

    @GET
    @Route(RouteType.PRIVATE)
    @Path("/second")
    @Produces(MediaType.APPLICATION_JSON)
    public int second() {
        return 0;
    }

    @GET
    @Route(gateways = {Constants.PRIVATE_GATEWAY_SERVICE, "quarkus-quickstart-test"})
    @Path("/third")
    @Produces(MediaType.APPLICATION_JSON)
    public int third() {
        return 0;
    }

    @GET
    @FacadeRoute(gateways = {Constants.PRIVATE_GATEWAY_SERVICE, "composite-gateway"})
    @Path("/fourth")
    @Produces(MediaType.APPLICATION_JSON)
    public int fourth() {
        return 0;
    }
}
