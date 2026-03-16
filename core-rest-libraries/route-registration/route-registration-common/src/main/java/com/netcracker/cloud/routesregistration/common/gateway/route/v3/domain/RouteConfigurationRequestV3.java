package com.netcracker.cloud.routesregistration.common.gateway.route.v3.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
@Builder
public class RouteConfigurationRequestV3 {
    private String namespace;
    private List<String> gateways;
    private List<VirtualService> virtualServices;

    public RouteConfigurationRequestV3 merge(RouteConfigurationRequestV3 anotherRequest) {
        if (gateways == null || gateways.size() != 1
                || anotherRequest.gateways == null || anotherRequest.gateways.size() != 1
                || !gateways.get(0).equals(anotherRequest.gateways.get(0))) {
            throw new IllegalStateException("Both RouteConfigurationRequestV3 must have exactly one same 'gateways' field value to be used in merge function");
        }

        anotherRequest.virtualServices.forEach(virtualService -> {
            Optional<VirtualService> virtualServiceOptional = virtualServices.stream()
                    .filter(thisVirtualService -> thisVirtualService.getName().equals(virtualService.getName()))
                    .findAny();
            if (virtualServiceOptional.isPresent()) {
                virtualServiceOptional.get().merge(virtualService);
            } else {
                virtualServices.add(virtualService);
            }
        });
        return this;
    }
}
