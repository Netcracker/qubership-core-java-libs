package org.qubership.cloud.bluegreen.quarkus.config;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.impl.service.ConsulBlueGreenStatePublisher;
import org.qubership.cloud.consul.provider.common.TokenStorage;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ConsulBlueGreenStatePublisherConfiguration {

    @ConfigProperty(name = "consul.url")
    String consulUrl;
    @ConfigProperty(name = "cloud.microservice.namespace")
    String namespace;

    @Produces
    @DefaultBean
    @ApplicationScoped
    @Named("blueGreenStatePublisher")
    public BlueGreenStatePublisher blueGreenStatePublisher(TokenStorage tokenStorage) {
        return new ConsulBlueGreenStatePublisher(tokenStorage::get, consulUrl, namespace);
    }

    public void close(@Disposes @Named("blueGreenStatePublisher") BlueGreenStatePublisher publisher) throws Exception {
        if (publisher instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }
}
