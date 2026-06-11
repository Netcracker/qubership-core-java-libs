package com.netcracker.routes.gateway.plugin;

import com.netcracker.cloud.routesregistration.common.annotation.Gateway;
import com.netcracker.cloud.routesregistration.common.annotation.Route;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;

@Route
@Gateway(RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1)
@Path(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1)
public class QuarkusTestController3 {

    @PUT
    @Route
    @Gateway({RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1, RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2})
    @Path(RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1)
    public void method1() {
    }
}
