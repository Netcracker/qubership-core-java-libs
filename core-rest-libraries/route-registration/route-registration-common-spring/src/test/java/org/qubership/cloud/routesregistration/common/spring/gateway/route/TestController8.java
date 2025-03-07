package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.qubership.cloud.routesregistration.common.annotation.FacadeRoute;
import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.qubership.cloud.routesregistration.common.gateway.route.RouteType.INTERNAL;
import static org.qubership.cloud.routesregistration.common.gateway.route.RouteType.PRIVATE;
import static org.qubership.cloud.routesregistration.common.gateway.route.RouteType.PUBLIC;

@RestController("TestController8")
@RequestMapping(path = {RoutesTestConfiguration.CLASS_ROUTES_8})
@Route(PUBLIC)
public class TestController8 {

    @FacadeRoute(timeout = RoutesTestConfiguration.TEST_TIMEOUT_1)
    @PostMapping(path = RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2)
    public void method15() {
    }

    @FacadeRoute
    @Route(PRIVATE)
    @GetMapping(path = RoutesTestConfiguration.METHOD_ROUTES_3)
    public void method17() {
    }

    @Route(INTERNAL)
    @PostMapping(path = RoutesTestConfiguration.METHOD_ROUTES_1)
    public void method14() {
    }

    @GetMapping
    public void method16() {
    }
}
