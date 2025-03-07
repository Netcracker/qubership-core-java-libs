package org.qubership.cloud.routesregistration.common.gateway.route.v3.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RouteDestination {
    private String cluster;
    private String endpoint;
}
