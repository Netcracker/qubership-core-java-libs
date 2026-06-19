package com.netcracker.routes.gateway.plugin;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController("TestController2")
public class SpringTestController2 {

    @RequestMapping(path = {RoutesTestConfiguration.METHOD_ROUTES_1 + "/{id}", RoutesTestConfiguration.METHOD_ROUTES_2}, method = RequestMethod.POST)
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
