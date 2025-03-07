package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.qubership.cloud.routesregistration.common.gateway.route.Constants;
import org.qubership.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("TestController14")
@RequestMapping(path = "/api/v1/ingress")
@GatewayRequestMapping("/api/v1/sample-service/ingress")
@Route(gateways = {RoutesTestConfiguration.INGRESS_GATEWAY, Constants.PUBLIC_GATEWAY_SERVICE}, hosts = "testHost")
public class TestController14 {

}