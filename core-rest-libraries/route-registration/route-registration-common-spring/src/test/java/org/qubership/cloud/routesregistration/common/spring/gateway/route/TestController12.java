package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.qubership.cloud.routesregistration.common.annotation.FacadeRoute;
import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.qubership.cloud.routesregistration.common.gateway.route.Constants;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("TestController12")
@RequestMapping(RoutesTestConfiguration.CLASS_ROUTES_12)
@Route(gateways = Constants.PUBLIC_GATEWAY_SERVICE)
public class TestController12 {

    @FacadeRoute
    @GetMapping
    public void publicAndDefaultFacadeRoute() {
    }

    @GetMapping(RoutesTestConfiguration.METHOD_ROUTES_1)
    @Route(gateways = RoutesTestConfiguration.INGRESS_GATEWAY)
    public void compositeGwRoute() {
    }

    @GetMapping(RoutesTestConfiguration.METHOD_ROUTES_2)
    @FacadeRoute(gateways = RoutesTestConfiguration.INGRESS_GATEWAY)
    public void anotherCompositeGwRoute() {
    }

    @GetMapping(RoutesTestConfiguration.METHOD_ROUTES_3)
    @Route(gateways = {Constants.PRIVATE_GATEWAY_SERVICE, RoutesTestConfiguration.INGRESS_GATEWAY})
    public void bothPrivateAndCompositeGwRoute() {
    }
}