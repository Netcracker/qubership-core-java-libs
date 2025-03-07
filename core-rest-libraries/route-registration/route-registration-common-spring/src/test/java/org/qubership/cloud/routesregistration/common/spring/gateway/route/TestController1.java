package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController("TestController1")
@RequestMapping(path = {RoutesTestConfiguration.CLASS_ROUTES_1, RoutesTestConfiguration.CLASS_ROUTES_2})
@Route(value = RouteType.PUBLIC)
public class TestController1 {


    @RequestMapping(path = {RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2}, method = RequestMethod.POST)
    @Route(value = RouteType.INTERNAL, timeout = RoutesTestConfiguration.TEST_TIMEOUT_1)
    public void method12() {
    }

    @RequestMapping(path = {RoutesTestConfiguration.METHOD_ROUTES_1, RoutesTestConfiguration.METHOD_ROUTES_2}, method = RequestMethod.POST)
    @Route(type = RouteType.PRIVATE, timeout = RoutesTestConfiguration.TEST_TIMEOUT_2)
    public void method11() {
    }
}
