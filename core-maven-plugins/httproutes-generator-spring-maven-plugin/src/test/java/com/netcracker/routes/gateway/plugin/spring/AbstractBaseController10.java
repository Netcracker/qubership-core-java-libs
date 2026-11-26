package com.netcracker.routes.gateway.plugin.spring;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(path = "/api/v1/base10")
public abstract class AbstractBaseController10 {

    @PostMapping(path = "/items")
    @Route
    public void addItem() {
    }
}
