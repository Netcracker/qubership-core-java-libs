package com.netcracker.cloud.routesregistration.common.gateway.route.v3.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RouteMatch {
    private String prefix;
}
