package com.netcracker.routes.gateway.plugin;

import com.netcracker.cloud.routesregistration.common.gateway.route.RouteEntry;

public record SourcedRoute(String className, RouteEntry entry) {
}
