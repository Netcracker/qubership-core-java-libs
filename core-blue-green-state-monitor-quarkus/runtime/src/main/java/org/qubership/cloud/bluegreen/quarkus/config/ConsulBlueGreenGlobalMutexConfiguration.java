package org.qubership.cloud.bluegreen.quarkus.config;

import org.qubership.cloud.bluegreen.api.service.GlobalMutexService;
import org.qubership.cloud.bluegreen.impl.service.ConsulGlobalMutexService;
import org.qubership.cloud.consul.provider.common.TokenStorage;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ConsulBlueGreenGlobalMutexConfiguration {

    @ConfigProperty(name = "consul.url")
    String consulUrl;

    @Produces
    @DefaultBean
    @ApplicationScoped
    public GlobalMutexService globalMutexService(TokenStorage tokenStorage) {
        return new ConsulGlobalMutexService(tokenStorage::get, consulUrl);
    }
}