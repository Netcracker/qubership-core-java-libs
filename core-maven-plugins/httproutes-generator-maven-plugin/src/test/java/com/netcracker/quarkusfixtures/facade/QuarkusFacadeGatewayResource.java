package com.netcracker.quarkusfixtures.facade;

import com.netcracker.cloud.routesregistration.common.annotation.FacadeGateway;
import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/sleep")
@FacadeGateway("/test/sleep")
@Route(RouteType.FACADE)
public class QuarkusFacadeGatewayResource {

    @POST
    public void method1() {
    }
}
