package org.qubership.maas.declarative.kafka.client.impl.tenant.api;

import java.util.List;
import java.util.function.Consumer;

// TODO added temporary, will be used default platform tenant client
public interface InternalTenantService {
    List<String> listAvailableTenants();

    void subscribe(Consumer<List<String>> callback);
}
