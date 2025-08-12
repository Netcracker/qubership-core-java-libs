package com.netcracker.cloud.bluegreen.quarkus.config;

import com.netcracker.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import com.netcracker.cloud.bluegreen.api.service.GlobalMutexService;
import com.netcracker.cloud.bluegreen.api.service.MicroserviceMutexService;
import com.netcracker.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import com.netcracker.cloud.bluegreen.impl.service.InMemoryGlobalMutexService;
import com.netcracker.cloud.bluegreen.impl.service.InMemoryMicroserviceMutexService;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class InMemoryConfiguration {

    @ConfigProperty(name = "cloud.microservice.namespace")
    String namespace;

    @Produces
    @DefaultBean
    @ApplicationScoped
    public BlueGreenStatePublisher inMemoryBlueGreenStatePublisher() {
        return new InMemoryBlueGreenStatePublisher(namespace);
    }

    @Produces
    @DefaultBean
    @ApplicationScoped
    public GlobalMutexService inMemoryGlobalMutexService() {
        return new InMemoryGlobalMutexService();
    }

    @Produces
    @DefaultBean
    @ApplicationScoped
    public MicroserviceMutexService imMemoryMicroserviceMutexService() {
        return new InMemoryMicroserviceMutexService();
    }
}
