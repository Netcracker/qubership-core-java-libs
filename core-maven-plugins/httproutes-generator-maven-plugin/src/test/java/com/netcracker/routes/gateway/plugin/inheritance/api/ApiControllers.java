package com.netcracker.routes.gateway.plugin.inheritance.api;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A controller that implements a mapped interface, as generated from an OpenAPI spec.
 */
public final class ApiControllers {

    private ApiControllers() {
    }

    @RequestMapping("/api/v1/api")
    public interface Api {
        @GetMapping("/items")
        @Route(RouteType.PUBLIC)
        @Route(RouteType.PRIVATE)
        void items();

        @GetMapping("/orders")
        void orders();
    }

    @RestController
    public static class Impl implements Api {
        @Override
        public void items() {
        }

        @Override
        @Route(RouteType.PRIVATE)
        public void orders() {
        }
    }
}
