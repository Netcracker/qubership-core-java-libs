package com.netcracker.routes.gateway.plugin.e2e.unforbidden;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The resource controller of the {@code @ForbiddenRoute} example in the plugin README, without
 * {@code @ForbiddenRoute}.
 */
@RestController
@RequestMapping("/resource")
@GatewayRequestMapping("/api/v1/my-service/resource")
@Route(RouteType.PUBLIC)
public class ResourceController {

    @GetMapping("/{var1}")
    @GatewayRequestMapping("/{var1}")
    @Route(RouteType.PUBLIC)
    public void get() {
    }

    @GetMapping("/{var1}/internal-api")
    @GatewayRequestMapping("/{var1}/internal-api")
    @Route(RouteType.INTERNAL)
    public void internalApi() {
    }

    @GetMapping("/{var1}/internal-api/status")
    @GatewayRequestMapping("/{var1}/internal-api/status")
    @Route(RouteType.PUBLIC)
    public void status() {
    }
}
