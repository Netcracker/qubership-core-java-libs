package org.qubership.cloud.routesregistration.common.gateway.route.v3;

import org.qubership.cloud.routesregistration.common.gateway.route.rest.DeleteDomainsRequest;
import org.qubership.cloud.routesregistration.common.gateway.route.v3.domain.DeleteDomainConfigurationV3;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteDomainsRequestV3 implements DeleteDomainsRequest {
    private List<DeleteDomainConfigurationV3> payload;

    @Override
    public String getUrl() {
        return "/api/v3/domains";
    }

    @Override
    public String getMethod() {
        return "DELETE";
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
