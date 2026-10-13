package com.netcracker.routes.gateway.plugin.inheritance.quarkus;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;


@Path("/x-case2")
public class Case2 implements ICase1 {
    @Override
    @Route
    public void method1() {

    }

    @Override
    public void method2() {

    }

    @Route
    @GET
    @Path("/case2/case2Method1")
    public void case2Method1() {

    }

    @Route
    @GET
    @Path("/case2/case2Method2")
    public void case2Method2() {

    }
}
