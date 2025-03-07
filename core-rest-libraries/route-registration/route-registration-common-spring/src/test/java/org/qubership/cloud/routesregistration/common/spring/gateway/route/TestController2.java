package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController("TestController2")
public class TestController2 {


    @RequestMapping(path = {RoutesTestConfiguration.METHOD_ROUTES_1, RoutesTestConfiguration.METHOD_ROUTES_2}, method = RequestMethod.POST)
    @Route
    public void method11() {
    }

    @RequestMapping(path = RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2, method = RequestMethod.POST)
    @Route(type = RouteType.PRIVATE)
    public void method12() {
    }

    /* duplicate route*/
    @RequestMapping(path = RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2, method = RequestMethod.GET)
    @Route(RouteType.PRIVATE)
    public void method13() {
    }

}
