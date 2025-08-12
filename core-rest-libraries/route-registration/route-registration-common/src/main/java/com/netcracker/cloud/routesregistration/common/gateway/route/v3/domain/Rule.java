package com.netcracker.cloud.routesregistration.common.gateway.route.v3.domain;

import lombok.Builder;
import lombok.Data;

import java.util.Collection;
import java.util.List;

@Data
@Builder
public class Rule {
    private RouteMatch match;
    private String prefixRewrite;
    private List<HeaderDefinition> addHeaders;
    private Collection<String> removeHeaders;
    private Boolean allowed;
    private Long timeout;
}
