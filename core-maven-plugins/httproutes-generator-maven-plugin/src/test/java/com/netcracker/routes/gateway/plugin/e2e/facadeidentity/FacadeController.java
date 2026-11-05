package com.netcracker.routes.gateway.plugin.e2e.facadeidentity;

import com.netcracker.cloud.routesregistration.common.annotation.FacadeRoute;
import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The facade and composite routes of {@code e2e.facade} without any rewrite.
 */
@RestController
public class FacadeController {

    @GetMapping("/items")
    @FacadeRoute
    public void items() {
    }

    @GetMapping("/orders")
    @Route(RouteType.FACADE)
    public void orders() {
    }

    @GetMapping("/shared")
    @Route(RouteType.PUBLIC)
    @Route(gateways = "composite-gw", hosts = "example.com")
    public void shared() {
    }
}
