package org.qubership.cloud.routesregistration.common.gateway.route.v3;

import org.qubership.cloud.routesregistration.common.gateway.route.Utils;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.CommonRequest;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.CompositeRequest;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.RegistrationRequest;
import org.qubership.cloud.routesregistration.common.gateway.route.v3.domain.DeleteDomainConfigurationV3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CompositeRequestV3 implements CompositeRequest<CommonRequest> {
    private final List<CommonRequest> requests = new ArrayList<>();

    public CompositeRequestV3(List<RegistrationRequest> registrationRequests, List<DeleteDomainConfigurationV3> deleteDomainsRequests) {
        if (Utils.isNotEmpty(registrationRequests)) {
            requests.addAll(registrationRequests);
        }
        if (Utils.isNotEmpty(deleteDomainsRequests)) {
            requests.add(new DeleteDomainsRequestV3(deleteDomainsRequests));
        }
    }

    @Override
    public Iterator<CommonRequest> iterator() {
        return requests.iterator();
    }
}
