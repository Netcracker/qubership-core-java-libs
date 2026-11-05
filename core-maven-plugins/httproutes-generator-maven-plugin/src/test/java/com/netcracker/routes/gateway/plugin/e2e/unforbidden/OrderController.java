package com.netcracker.routes.gateway.plugin.e2e.unforbidden;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The order controller of the {@code @ForbiddenRoute} example in the plugin README, without
 * {@code @ForbiddenRoute}.
 */
@RestController
@RequestMapping("/order")
@GatewayRequestMapping("/api/v1/my-service/order")
public class OrderController {

    @GetMapping("/{var1}/items")
    @GatewayRequestMapping("/{var1}/items")
    @Route(RouteType.PUBLIC)
    public void items() {
    }
}
