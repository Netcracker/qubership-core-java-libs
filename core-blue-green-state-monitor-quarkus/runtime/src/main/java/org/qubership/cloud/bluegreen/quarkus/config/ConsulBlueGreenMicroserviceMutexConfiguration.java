package org.qubership.cloud.bluegreen.quarkus.config;

import org.qubership.cloud.bluegreen.api.service.MicroserviceMutexService;
import org.qubership.cloud.bluegreen.impl.service.ConsulMicroserviceMutexService;
import org.qubership.cloud.bluegreen.impl.util.EnvUtil;
import org.qubership.cloud.consul.provider.common.TokenStorage;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
public class ConsulBlueGreenMicroserviceMutexConfiguration {

    @ConfigProperty(name = "consul.url")
    String consulUrl;
    @ConfigProperty(name = "cloud.microservice.namespace")
    String namespace;
    @ConfigProperty(name = "cloud.microservice.name")
    String name;
    @ConfigProperty(name = "pod.name")
    Optional<String> pod;

    @Produces
    @DefaultBean
    @ApplicationScoped
    @Named("microserviceMutexService")
    public MicroserviceMutexService microserviceMutexService(TokenStorage tokenStorage) {
        String podName = pod.orElseGet(EnvUtil::getPodName);
        return new ConsulMicroserviceMutexService(tokenStorage::get, consulUrl, namespace, name, podName);
    }

    public void close(@Disposes @Named("microserviceMutexService") MicroserviceMutexService service) throws Exception {
        if (service instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }
}