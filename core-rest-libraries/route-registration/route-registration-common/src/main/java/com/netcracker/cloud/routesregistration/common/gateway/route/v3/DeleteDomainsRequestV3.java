package com.netcracker.cloud.routesregistration.common.gateway.route.v3;

import com.netcracker.cloud.routesregistration.common.gateway.route.rest.DeleteDomainsRequest;
import com.netcracker.cloud.routesregistration.common.gateway.route.v3.domain.DeleteDomainConfigurationV3;
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
