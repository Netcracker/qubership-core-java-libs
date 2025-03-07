package org.qubership.cloud.routesregistration.common.gateway.route.v3;

import org.qubership.cloud.routesregistration.common.gateway.route.rest.RegistrationRequest;
import org.qubership.cloud.routesregistration.common.gateway.route.v3.domain.RouteConfigurationRequestV3;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestV3 implements RegistrationRequest {
    private RouteConfigurationRequestV3 payload;

    @Override
    public String getUrl() {
        return "/api/v3/routes";
    }

    @Override
    public String getMethod() {
        return "POST";
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
