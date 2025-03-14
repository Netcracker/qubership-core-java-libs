package org.qubership.cloud.maas.client.quarkus;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@ApplicationScoped
public class MaasLocalDevConfig {

    @Singleton
    @Produces
    @IfBuildProperty(name = "maas.local-dev.enabled", stringValue = "true")
    public BlueGreenStatePublisher inMemoryBlueGreenStatePublisher() {
        return new InMemoryBlueGreenStatePublisher();
    }
}
