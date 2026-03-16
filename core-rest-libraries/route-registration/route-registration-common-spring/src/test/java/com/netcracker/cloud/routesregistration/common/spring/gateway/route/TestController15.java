package com.netcracker.cloud.routesregistration.common.spring.gateway.route;

import com.netcracker.cloud.routesregistration.common.annotation.FacadeRoute;
import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("TestController15")
@RequestMapping(path = "/api/v1/ingress")
@GatewayRequestMapping("/api/v1/sample-service/ingress")
@Route(gateways = RoutesTestConfiguration.INGRESS_GATEWAY, hosts = "testHost")
public class TestController15 {

    @Route(gateways = RoutesTestConfiguration.INGRESS_GATEWAY)
    @GetMapping("/inner")
    public ResponseEntity innerRequestDetails() {
        return null;
    }

    @FacadeRoute(gateways = RoutesTestConfiguration.INGRESS_GATEWAY)
    @GetMapping("/facade")
    public ResponseEntity facadeRequestDetails() {
        return null;
    }
}
