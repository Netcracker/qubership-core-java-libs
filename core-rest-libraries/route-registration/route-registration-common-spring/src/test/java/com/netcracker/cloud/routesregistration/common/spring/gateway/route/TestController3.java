package com.netcracker.cloud.routesregistration.common.spring.gateway.route;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController("TestController3")
@Route
@GatewayRequestMapping({RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_2})
@RequestMapping(path = RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1)
public class TestController3 {

    @Route
    @GatewayRequestMapping(path = {RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1, RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2})
    @RequestMapping(path = RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, method = RequestMethod.POST)
    public void method1() {
    }

}
