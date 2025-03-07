package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.qubership.cloud.routesregistration.common.gateway.route.RouteType.PRIVATE;
import static org.qubership.cloud.routesregistration.common.gateway.route.RouteType.PUBLIC;


@RestController("TestController5")
@RequestMapping(path = RoutesTestConfiguration.CLASS_ROUTES_3)
@Route(PUBLIC)
public class TestControllerBaseFor5 {

    //@RequestMapping(path = CLASS_ROUTES_3 + METHOD_ROUTES_1, method = RequestMethod.POST)
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