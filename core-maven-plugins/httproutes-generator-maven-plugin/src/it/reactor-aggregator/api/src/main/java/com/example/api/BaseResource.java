package com.example.api;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/base-it")
public abstract class BaseResource {

    @GET
    @Route
    public abstract void method1();
}
