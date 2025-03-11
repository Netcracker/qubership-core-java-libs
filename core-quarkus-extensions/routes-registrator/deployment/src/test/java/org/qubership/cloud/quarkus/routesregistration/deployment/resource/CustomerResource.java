package org.qubership.cloud.quarkus.routesregistration.deployment.resource;


import org.qubership.cloud.routesregistration.common.annotation.FacadeRoute;
import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Route(RouteType.PRIVATE)
@FacadeRoute
@Path("/test")
public class CustomerResource {
    @GET
    @Path("/private")
    @Produces(MediaType.APPLICATION_JSON)
    public int publicRoute() {
        return 0;
    }

    @GET
    @Path("/public")
    @Route(RouteType.PUBLIC)
    @Produces(MediaType.APPLICATION_JSON)
    public int privateRoute() {
        return 0;
    }

    @GET
    @Path("/facade")
    @FacadeRoute
    @Produces(MediaType.APPLICATION_JSON)
    public int facadeRoute() {
        return 0;
    }
}
