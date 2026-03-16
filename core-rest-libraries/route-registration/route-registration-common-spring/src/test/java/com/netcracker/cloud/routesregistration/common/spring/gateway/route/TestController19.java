package com.netcracker.cloud.routesregistration.common.spring.gateway.route;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("TestController16")
@RequestMapping(path = "/api/v1/ingress")
@Route(type = RouteType.FACADE, hosts = "*")
@Route(type = RouteType.FACADE, hosts = "testHost")
public class TestController19 {
}
