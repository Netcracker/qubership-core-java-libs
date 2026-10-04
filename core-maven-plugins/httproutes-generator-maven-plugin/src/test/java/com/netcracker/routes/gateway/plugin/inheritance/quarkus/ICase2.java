package com.netcracker.routes.gateway.plugin.inheritance.quarkus;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/x-icase2")
public interface ICase2 extends ICase3 {
    @GET
    @Path("/icase2/method1")
    void method1();

    @GET
    @Path("/icase2/method2")
    void method2();

    @GET
    @Path("/icase2/method3")
    void method3();

    @GET
    @Path("/icase2/method5")
    void method5();
}
