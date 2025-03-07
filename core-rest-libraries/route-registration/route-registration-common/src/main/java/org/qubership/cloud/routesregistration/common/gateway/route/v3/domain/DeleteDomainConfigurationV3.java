package org.qubership.cloud.routesregistration.common.gateway.route.v3.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DeleteDomainConfigurationV3 {
    String virtualService;
    String gateway;
    List<String> domains;
}
