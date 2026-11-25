package com.netcracker.routes.gateway.plugin.spring;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/svc13")
public class Controller13RepeatedRoute {

    @RequestMapping(path = "/items")
    @Route(RouteType.PUBLIC)
    @Route(RouteType.FACADE)
    public void method1() {
    }
}
