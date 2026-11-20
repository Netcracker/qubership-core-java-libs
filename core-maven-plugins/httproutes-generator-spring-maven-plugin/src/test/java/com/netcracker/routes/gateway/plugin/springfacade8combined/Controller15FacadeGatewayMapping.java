package com.netcracker.routes.gateway.plugin.springfacade8combined;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.FacadeGatewayRequestMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/svc")
@FacadeGatewayRequestMapping("/facade")
@Route(RouteType.FACADE)
public class Controller15FacadeGatewayMapping {

    @RequestMapping(method = RequestMethod.GET)
    public void method1() {
    }
}
