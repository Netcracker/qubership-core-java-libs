package com.netcracker.routes.gateway.plugin.jaxrsguard;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/jaxrs-guard")
public class JaxRsGuardResource {

    @GET
    public String ok() {
        return "ok";
    }
}