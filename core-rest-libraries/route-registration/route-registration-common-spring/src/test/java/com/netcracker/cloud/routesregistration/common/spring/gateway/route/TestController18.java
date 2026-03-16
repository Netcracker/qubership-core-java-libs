package com.netcracker.cloud.routesregistration.common.spring.gateway.route;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("TestController16")
@RequestMapping(path = "/api/v1/ingress")
@Route(type = RouteType.FACADE)
@Route(type = RouteType.FACADE, gateways = RoutesTestConfiguration.DEFAULT_INGRESS_GATEWAY)
@Route(type = RouteType.FACADE, gateways = RoutesTestConfiguration.INGRESS_GATEWAY, hosts = "testHost")
@Route(value = RouteType.INTERNAL)
public class TestController18 {

    @Route(type = RouteType.FACADE)
    @Route(type = RouteType.FACADE, gateways = RoutesTestConfiguration.DEFAULT_INGRESS_GATEWAY)
    @Route(type = RouteType.FACADE, gateways = RoutesTestConfiguration.INGRESS_GATEWAY, hosts = "testHost")
    @GetMapping("/facade")
    public ResponseEntity facadeRequestDetails() {
        return null;
    }
}
