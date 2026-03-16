package com.netcracker.cloud.routesregistration.common.annotation.processing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RouteHostMapping {
    private String gatewayName;
    private List<String> virtualHosts;

    public boolean isEmpty() {
        return gatewayName == null || gatewayName.isBlank() || virtualHosts == null || virtualHosts.isEmpty();
    }
}
