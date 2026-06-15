package com.netcracker.routes.gateway.plugin;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path(RoutesTestConfiguration.CLASS_ROUTES_1)
@Route(value = RouteType.PUBLIC)
public class QuarkusTestController1 {

    @POST
    @Path(RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2)
    @Route(value = RouteType.INTERNAL, timeout = RoutesTestConfiguration.TEST_TIMEOUT_1)
    public void method12() {
    }

    @POST
    @Path(RoutesTestConfiguration.METHOD_ROUTES_1)
    @Route(type = RouteType.PRIVATE, timeout = RoutesTestConfiguration.TEST_TIMEOUT_2)
    public void method11() {
    }
}
