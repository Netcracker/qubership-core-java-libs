package com.netcracker.routes.gateway.plugin;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/base")
public class QuarkusBaseResource {

    @POST
    @Route
    public void method1() {
    }
}
