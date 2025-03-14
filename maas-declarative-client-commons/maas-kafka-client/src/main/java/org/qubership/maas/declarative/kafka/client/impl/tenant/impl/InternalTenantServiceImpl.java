package org.qubership.maas.declarative.kafka.client.impl.tenant.impl;

import org.qubership.cloud.tenantmanager.client.Tenant;
import org.qubership.cloud.tenantmanager.client.TenantManagerConnector;
import org.qubership.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class InternalTenantServiceImpl implements InternalTenantService {

    //TODO impl pull tenant service or use tenantManagerConnector
    private final TenantManagerConnector tenantManagerConnector;

    public InternalTenantServiceImpl(TenantManagerConnector tenantManagerConnector) {
        this.tenantManagerConnector = tenantManagerConnector;
    }

    @Override
    public List<String> listAvailableTenants() {
        return tenantManagerConnector.getTenantList().stream().map(tenant -> tenant.getExternalId()).collect(Collectors.toList());
    }

    @Override
    public void subscribe(Consumer<List<String>> callback) {
        tenantManagerConnector.subscribe(tenants -> {
            callback.accept(tenants.stream().map(Tenant::getExternalId).collect(Collectors.toList()));
        });
    }
}
