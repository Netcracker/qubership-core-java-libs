package com.netcracker.cloud.routesregistration.common.gateway.route.v3.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VirtualService {
    private String name;
    private List<String> hosts;
    private List<HeaderDefinition> addHeaders;
    private List<String> removeHeaders;
    private RouteConfig routeConfiguration;

    public VirtualService merge(VirtualService anotherService) {
        if (!name.equals(anotherService.getName())) {
            throw new IllegalArgumentException("Cannot merge VirtualServices with different names");
        }
        routeConfiguration.merge(anotherService.getRouteConfiguration());
        return this;
    }
}
