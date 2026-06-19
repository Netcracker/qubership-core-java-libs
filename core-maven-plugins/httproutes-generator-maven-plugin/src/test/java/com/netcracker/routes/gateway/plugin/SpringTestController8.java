package com.netcracker.routes.gateway.plugin;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController("FooBarController")
@RequestMapping(path = RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1)
@GatewayRequestMapping(path = RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1)
@Route(value = RouteType.PUBLIC)
public class SpringTestController8 {

    @RequestMapping(path = RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, method = RequestMethod.POST)
    @GatewayRequestMapping({RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1})
    @Route(value = RouteType.FACADE, timeout = 10000)
    public void method1() {
    }

    @RequestMapping(path = RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_2, method = RequestMethod.POST)
    @Route(type = RouteType.PRIVATE, timeout = 20000)
    public void method2() {
    }
}
