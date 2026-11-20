package com.netcracker.routes.gateway.plugin;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/svc-ignored")
@GatewayRequestMapping("/gw-ignored")
@Route
public class QuarkusGatewayRequestMappingIgnoredResource {

    @POST
    public void method1() {
    }
}
