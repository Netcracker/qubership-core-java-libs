package org.qubership.cloud.maas.client.quarkus;

import org.qubership.cloud.maas.client.api.MaaSAPIClient;
import org.qubership.cloud.maas.client.impl.MaaSAPIClientImpl;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Dependent
public class MaaSClientCommonConfiguration {
    @Produces
    @DefaultBean
    @Singleton
    public MaaSAPIClient getMaaSAPIClient() {
        return new MaaSAPIClientImpl(() -> "fake");
    }
}
