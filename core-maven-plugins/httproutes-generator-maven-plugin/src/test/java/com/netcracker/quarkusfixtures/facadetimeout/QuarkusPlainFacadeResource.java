package com.netcracker.quarkusfixtures.facadetimeout;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/api/v1/reports")
@Route(value = RouteType.FACADE, timeout = 300000)
public class QuarkusPlainFacadeResource {

    @POST
    public void method1() {
    }
}
