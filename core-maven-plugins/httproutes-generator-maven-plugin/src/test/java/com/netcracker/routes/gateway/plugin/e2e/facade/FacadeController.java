package com.netcracker.routes.gateway.plugin.e2e.facade;

import com.netcracker.cloud.routesregistration.common.annotation.FacadeRoute;
import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.FacadeGatewayRequestMapping;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Facade and composite routes, one of them with a rewrite.
 */
@RestController
public class FacadeController {

    @GetMapping("/items")
    @FacadeGatewayRequestMapping("/facade/items")
    @FacadeRoute
    public void items() {
    }

    @GetMapping("/orders")
    @Route(RouteType.FACADE)
    public void orders() {
    }

    @GetMapping("/shared")
    @GatewayRequestMapping("/api/v1/svc/shared")
    @Route(RouteType.PUBLIC)
    @Route(gateways = "composite-gw", hosts = "example.com")
    public void shared() {
    }
}
