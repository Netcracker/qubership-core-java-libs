package org.qubership.cloud.quarkus.routesregistration.deployment.resource;


import org.qubership.cloud.routesregistration.common.annotation.Route;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Route(timeout = 1)
@Path("/with-timeout")
public class CustomTimeoutResource {
    @GET
    @Path("/first")
    @Route(timeout = 2)
    @Produces(MediaType.APPLICATION_JSON)
    public int publicRoute() {
        return 0;
    }

    @GET
    @Path("/second")
    @Produces(MediaType.APPLICATION_JSON)
    public int privateRoute() {
        return 0;
    }
}
