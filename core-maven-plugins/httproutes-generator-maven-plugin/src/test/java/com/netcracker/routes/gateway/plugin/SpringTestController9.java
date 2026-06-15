package com.netcracker.routes.gateway.plugin;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1)
@Route(RouteType.PUBLIC)
@GatewayRequestMapping(RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1)
public class SpringTestController9 {

    @RequestMapping(method = RequestMethod.GET)
    public void method1() {
    }
}
