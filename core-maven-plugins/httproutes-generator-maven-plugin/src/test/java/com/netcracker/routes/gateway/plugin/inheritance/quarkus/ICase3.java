package com.netcracker.routes.gateway.plugin.inheritance.quarkus;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/x-icase3")
public interface ICase3 {
    @GET
    @Path("/icase3/method3")
    void method3();

    @GET
    @Path("/icase3/method4")
    void method4();

    @GET
    @Path("/icase3/method4String")
    void method4(String s);

    @GET
    @Path("/icase3/method4Integer")
    void method4(Integer i);
}
