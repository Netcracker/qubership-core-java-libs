package com.netcracker.routes.gateway.plugin.springplaceholder;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "${api.prefix}/users")
@Route
public class ControllerWithPlaceholder {
}
