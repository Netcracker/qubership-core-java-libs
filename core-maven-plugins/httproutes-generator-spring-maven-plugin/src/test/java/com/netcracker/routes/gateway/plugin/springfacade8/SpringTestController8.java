package com.netcracker.routes.gateway.plugin.springfacade8;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController("FooBarController")
@RequestMapping(path = "/class/path/to")
@GatewayRequestMapping(path = "/class/path/from")
@Route(value = RouteType.PUBLIC)
public class SpringTestController8 {

    @RequestMapping(path = "/method/path/to", method = RequestMethod.POST)
    @GatewayRequestMapping({"/method/path/from"})
    @Route(value = RouteType.FACADE, timeout = 10000)
    public void method1() {
    }

    @RequestMapping(path = "/method/path/to2", method = RequestMethod.POST)
    @Route(type = RouteType.PRIVATE, timeout = 20000)
    public void method2() {
    }
}
