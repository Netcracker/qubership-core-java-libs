package com.netcracker.routes.gateway.plugin.inheritance.parentcontext;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Subclasses of an abstract base controller whose class annotations they inherit or replace.
 */
public final class ParentContextControllers {

    private ParentContextControllers() {
    }

    @RequestMapping("/base")
    @GatewayRequestMapping("/api/v1/base")
    @Route(RouteType.PUBLIC)
    public abstract static class Base {
        @GetMapping("/items")
        @Route(RouteType.PRIVATE)
        public void items() {
        }
    }

    @RestController
    public static class Child extends Base {
    }

    @RestController
    @RequestMapping("/second")
    public static class Second extends Base {
        @Override
        @Route(RouteType.INTERNAL)
        public void items() {
        }
    }
}
