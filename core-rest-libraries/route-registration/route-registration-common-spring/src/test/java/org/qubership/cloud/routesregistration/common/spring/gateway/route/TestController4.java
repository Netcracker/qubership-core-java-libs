package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteType;
import org.qubership.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController("TestController4")
@Validated
public class TestController4 {

    @GatewayRequestMapping({RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1, RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2})
    @RequestMapping(path = {RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1}, method = RequestMethod.POST)
    @Route(value = RouteType.PUBLIC)
    public void method1() {
    }

    @GatewayRequestMapping({RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1, RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2})
    @RequestMapping(path = {RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_2}, method = RequestMethod.POST)
    @Route(RouteType.PRIVATE)
    public void method2() {
    }

}
