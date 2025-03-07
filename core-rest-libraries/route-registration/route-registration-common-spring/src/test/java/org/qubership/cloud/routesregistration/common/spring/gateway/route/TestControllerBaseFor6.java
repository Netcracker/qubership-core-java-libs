package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.qubership.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.qubership.cloud.routesregistration.common.spring.gateway.route.RoutesTestConfiguration.CLASS_ROUTES_4;
import static org.qubership.cloud.routesregistration.common.spring.gateway.route.RoutesTestConfiguration.METHOD_ROUTES_1;
import static org.qubership.cloud.routesregistration.common.gateway.route.RouteType.PUBLIC;

@RestController("TestController5")
@RequestMapping(path = CLASS_ROUTES_4)
public class TestControllerBaseFor6 {

    @RequestMapping(path = METHOD_ROUTES_1, method = RequestMethod.POST)
    @GatewayRequestMapping("/custom" + METHOD_ROUTES_1)
    @Route(PUBLIC)
    public void method1() {
    }

}