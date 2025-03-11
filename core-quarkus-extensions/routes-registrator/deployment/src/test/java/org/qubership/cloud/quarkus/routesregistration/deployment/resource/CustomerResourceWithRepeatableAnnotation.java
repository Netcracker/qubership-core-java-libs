package org.qubership.cloud.quarkus.routesregistration.deployment.resource;


import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteType;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Route(RouteType.PRIVATE)
@Route(value = RouteType.FACADE, gateways = "testFirstGateway", hosts = "testFirstVHost")
@Path("/testRouteController")
public class CustomerResourceWithRepeatableAnnotation {

    @GET
    @Path("/testRouteMethod")
    @Route(RouteType.PUBLIC)
    @Route(value = RouteType.FACADE, gateways = "testSecondGateway", hosts = "testSecondVHost")
    @Produces(MediaType.APPLICATION_JSON)
    public int route() {
        return 0;
    }
}
