package com.netcracker.routes.gateway.plugin.inheritance.childcontext;

import com.netcracker.cloud.routesregistration.common.annotation.ForbiddenRoute;
import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * An abstract base controller without a request mapping, mapped by the request mapping of its subclass.
 */
public final class ChildContextControllers {

    private ChildContextControllers() {
    }

    @ForbiddenRoute(RouteType.PUBLIC)
    public abstract static class Base {
        @GetMapping("/items")
        @Route(RouteType.PRIVATE)
        public void items() {
        }
    }

    @RestController
    @RequestMapping("/api/v1/child")
    public static class Child extends Base {
    }
}
