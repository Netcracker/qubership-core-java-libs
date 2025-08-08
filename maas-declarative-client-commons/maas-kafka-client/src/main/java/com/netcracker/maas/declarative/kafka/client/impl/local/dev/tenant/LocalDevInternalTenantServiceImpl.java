package com.netcracker.maas.declarative.kafka.client.impl.local.dev.tenant;

import com.netcracker.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// LocalDev only
public class LocalDevInternalTenantServiceImpl implements InternalTenantService {

    private final List<String> localDevTenantIds;

    public LocalDevInternalTenantServiceImpl(Supplier<List<String>> localDevTenantIdsSupplier) {
        this.localDevTenantIds = localDevTenantIdsSupplier.get()
                .stream()
                .map(String::trim)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listAvailableTenants() {
        return localDevTenantIds;
    }

    @Override
    public void subscribe(Consumer<List<String>> callback) {
        // Ignore it
    }
}
