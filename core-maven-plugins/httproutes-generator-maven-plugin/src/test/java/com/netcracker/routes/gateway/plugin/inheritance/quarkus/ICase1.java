package com.netcracker.routes.gateway.plugin.inheritance.quarkus;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/x-icase1")
public interface ICase1 {
    @GET
    @Path("/icase1/method1")
    void method1();

    @GET
    @Path("/icase1/method2")
    void method2();
}
