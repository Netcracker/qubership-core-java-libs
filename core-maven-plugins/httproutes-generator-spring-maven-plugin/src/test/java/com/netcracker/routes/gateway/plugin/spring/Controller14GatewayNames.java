package com.netcracker.routes.gateway.plugin.spring;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/svc14")
public class Controller14GatewayNames {

    @RequestMapping(path = "/border")
    @Route(gateways = "private-gateway-service")
    public void borderGateway() {
    }

    @RequestMapping(path = "/composite")
    @Route(gateways = "my-composite-gateway", hosts = "example.com")
    public void compositeGateway() {
    }
}
