package com.netcracker.cloud.bluegreen.quarkus.config;

import com.netcracker.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import com.netcracker.cloud.bluegreen.impl.service.ConsulBlueGreenStatePublisher;
import com.netcracker.cloud.consul.provider.common.ConsulTokenSource;
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
    public BlueGreenStatePublisher blueGreenStatePublisher(ConsulTokenSource tokenSource) {
        return new ConsulBlueGreenStatePublisher(tokenSource::get, consulUrl, namespace, tokenSource::reportRefusal);
    }

    public void close(@Disposes @Named("blueGreenStatePublisher") BlueGreenStatePublisher publisher) throws Exception {
        if (publisher instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }
}
