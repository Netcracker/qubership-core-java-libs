package com.netcracker.routes.gateway.plugin;

import com.netcracker.cloud.routesregistration.common.annotation.Gateway;
import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Route(RouteType.PUBLIC)
@Gateway("test/sleep")
@Path("/sleep")
public class QuarkusTestController4 {

    @POST
    public void method1() {
    }
}
