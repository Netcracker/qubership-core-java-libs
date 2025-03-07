package org.qubership.cloud.routesregistration.common.gateway.route.v3.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeaderDefinition {
    private String name;
    private String value;
}
