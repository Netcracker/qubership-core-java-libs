package com.netcracker.cloud.routesregistration.common.spring.gateway.route;

import com.netcracker.cloud.routesregistration.common.annotation.FacadeRoute;
import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.FacadeGatewayRequestMapping;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.netcracker.cloud.routesregistration.common.gateway.route.RouteType.INTERNAL;
import static com.netcracker.cloud.routesregistration.common.gateway.route.RouteType.PRIVATE;

@RestController("TestController9")
@GatewayRequestMapping(value = "/gateway/v1")
@RequestMapping(path = RoutesTestConfiguration.CLASS_ROUTES_1)
@Route(INTERNAL)
@FacadeRoute
public class TestController9 {

    @FacadeRoute(timeout = RoutesTestConfiguration.TEST_TIMEOUT_1)
    @PostMapping(path = RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2)
    public void method15() {
    }


    @FacadeRoute
    @FacadeGatewayRequestMapping(value = "/api/v2")
    @Route(PRIVATE)
    @GatewayRequestMapping(value = "/api/v3")
    @GetMapping(path = RoutesTestConfiguration.METHOD_ROUTES_3)
    public void method17() {
    }

    @PostMapping(path = RoutesTestConfiguration.METHOD_ROUTES_1)
    public void method14() {
    }
}
