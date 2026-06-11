package com.netcracker.routes.gateway.plugin;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.netcracker.cloud.routesregistration.common.gateway.route.RouteType.PRIVATE;
import static com.netcracker.cloud.routesregistration.common.gateway.route.RouteType.PUBLIC;


@RestController("TestController5")
@RequestMapping(path = RoutesTestConfiguration.CLASS_ROUTES_3)
@Route(PUBLIC)
public class SpringTestControllerBaseFor5 {

    @PostMapping(path = RoutesTestConfiguration.METHOD_ROUTES_1)
    @Route(PUBLIC)
    public void methodPost() {
    }

    @GetMapping(path = RoutesTestConfiguration.METHOD_ROUTES_2)
    @Route
    public void methodGet() {
    }

    @GetMapping(path = RoutesTestConfiguration.METHOD_ROUTES_3)
    @Route(PRIVATE)
    public void methodPut() {
    }
}
