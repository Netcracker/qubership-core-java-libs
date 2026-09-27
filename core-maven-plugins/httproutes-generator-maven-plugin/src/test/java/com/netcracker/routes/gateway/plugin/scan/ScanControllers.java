package com.netcracker.routes.gateway.plugin.scan;

import com.netcracker.cloud.routesregistration.common.annotation.FacadeRoute;
import com.netcracker.cloud.routesregistration.common.annotation.ForbiddenRoute;
import com.netcracker.cloud.routesregistration.common.annotation.Route;
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
    @ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE, RouteType.INTERNAL})
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
    @RequestMapping("/shared")
    @Route(RouteType.PUBLIC)
    public static class SharedBase {
    }

    @RestController
    @RequestMapping("/shared")
    @Route(RouteType.PUBLIC)
    public static class SharedChild extends SharedBase {
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
    public static class ForbiddenPartialSegment {
        @GetMapping("/files/{name}.txt")
        @ForbiddenRoute(RouteType.PUBLIC)
        public void file() {
        }
    }

    @RestController
    public static class ForbiddenWildcard {
        @GetMapping("/files/*")
        @ForbiddenRoute(RouteType.PUBLIC)
        public void files() {
        }
    }

    @RestController
    public static class ForbiddenRegexVariable {
        @GetMapping("/items/{id:\\d+}")
        @ForbiddenRoute(RouteType.PUBLIC)
        public void item() {
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
    public static class HostsOnBorderGateway {
        @GetMapping("/items")
        @Route(gateways = "public-gateway-service", hosts = "example.com")
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
