package com.netcracker.routes.gateway.plugin.springgatewaymapping;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/svc17")
@GatewayRequestMapping("/gw17")
@Route(RouteType.FACADE)
public class Controller17FacadeGatewayRequestMappingIgnored {

    @RequestMapping(method = RequestMethod.GET)
    public void method1() {
    }
}
