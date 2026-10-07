package com.netcracker.routes.gateway.plugin.scan;

import com.netcracker.cloud.routesregistration.common.annotation.FacadeRoute;
import com.netcracker.cloud.routesregistration.common.annotation.ForbiddenRoute;
import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.annotation.Routes;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.FacadeGatewayRequestMapping;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controllers for scanner tests of route declarations, {@code @ForbiddenRoute} and legacy annotation forms.
 */
public final class ScanControllers {

    private ScanControllers() {
    }

    @RestController
    @RequestMapping("/api/v1/svc/order")
    @ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE})
    public static class ForbiddenClassWithoutRoute {
        @GetMapping("/{id}/items")
        @Route(RouteType.PUBLIC)
        public void items() {
        }
    }

    @RestController
    public static class ForbiddenWithGatewayMapping {
        @GetMapping("/secret/{id}")
        @GatewayRequestMapping("/api/x/{id}/secret")
        @Route(RouteType.INTERNAL)
        @ForbiddenRoute(RouteType.PUBLIC)
        public void secret() {
        }
    }

    @RestController
    public static class ForbiddenEmpty {
        @GetMapping("/empty")
        @ForbiddenRoute({})
        public void empty() {
        }
    }

    @RestController
    public static class ForbiddenFacade {
        @GetMapping("/facade")
        @ForbiddenRoute(RouteType.FACADE)
        public void facade() {
        }
    }

    @RestController
    public static class ForbiddenInternal {
        @GetMapping("/internal")
        @ForbiddenRoute({RouteType.PUBLIC, RouteType.INTERNAL})
        public void internal() {
        }
    }

    @RestController
    public static class ForbiddenWithSeveralMappings {
        @GetMapping({"/a", "/b"})
        @ForbiddenRoute(RouteType.PUBLIC)
        public void paths() {
        }
    }

    @RestController
    @RequestMapping("/items")
    @Route(value = RouteType.PUBLIC, timeout = 5000)
    public static class BareMethodRouteInRouteClass {
        @GetMapping("/{id}")
        @Route
        public void item() {
        }
    }

    @RestController
    @RequestMapping("/items")
    @Routes({@Route(value = RouteType.PUBLIC, timeout = 5000)})
    public static class BareMethodRouteInRoutesClass {
        @GetMapping("/{id}")
        @Route
        public void item() {
        }
    }

    @RestController
    @RequestMapping("/items")
    @Route(RouteType.PUBLIC)
    public static class MethodWithoutRoute {
        @GetMapping("/{id}")
        public void item() {
        }
    }

    @RestController
    public static class ValueAndType {
        @GetMapping("/value")
        @Route(value = RouteType.PUBLIC, type = RouteType.PRIVATE)
        public void value() {
        }

        @GetMapping("/type")
        @Route(value = RouteType.INTERNAL, type = RouteType.PRIVATE)
        public void type() {
        }
    }

    @RestController
    public static class TwoRoutes {
        @GetMapping("/items")
        @Route(RouteType.PUBLIC)
        @Route(gateways = "composite-gw")
        public void items() {
        }
    }

    @RestController
    public static class BorderGatewayName {
        @GetMapping("/items")
        @Route(gateways = "private-gateway-service")
        public void items() {
        }
    }

    @RestController
    public static class CompositeGatewayName {
        @GetMapping("/items")
        @GatewayRequestMapping("/api/items")
        @Route(gateways = "my-service")
        public void items() {
        }
    }

    @RestController
    public static class FacadeWithBorderMapping {
        @GetMapping("/items")
        @GatewayRequestMapping("/api/items")
        @Route(RouteType.FACADE)
        public void items() {
        }
    }

    @RestController
    public static class FacadeRouteWithFacadeMapping {
        @GetMapping("/items")
        @FacadeGatewayRequestMapping("/facade/items")
        @FacadeRoute
        public void items() {
        }
    }

    @RestController
    @RequestMapping("/items")
    @Route(RouteType.PUBLIC)
    public static class MethodListReplacesClassList {
        @GetMapping("/{id}")
        @Route(RouteType.INTERNAL)
        public void item() {
        }
    }

    @RestController
    public static class EmptyGatewaysString {
        @GetMapping("/items")
        @Route(value = RouteType.PUBLIC, gateways = {""})
        public void items() {
        }
    }

    @RestController
    public static class HostsOnCompositeGateway {
        @GetMapping("/items")
        @Route(gateways = "composite-gw", hosts = "example.com")
        public void items() {
        }
    }

    @RestController
    public static class CompositeWithGatewayPath {
        @GetMapping("/items")
        @Route(gateways = "composite-gw", hosts = "example.com")
        @GatewayRequestMapping("/composite/items")
        public void items() {
        }
    }
}
