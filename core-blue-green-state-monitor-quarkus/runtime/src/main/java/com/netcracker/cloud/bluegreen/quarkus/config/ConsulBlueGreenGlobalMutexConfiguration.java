package com.netcracker.cloud.bluegreen.quarkus.config;

import com.netcracker.cloud.bluegreen.api.service.GlobalMutexService;
import com.netcracker.cloud.bluegreen.impl.service.ConsulGlobalMutexService;
import com.netcracker.cloud.consul.provider.common.ConsulTokenSource;
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
    public GlobalMutexService globalMutexService(ConsulTokenSource tokenSource) {
        return new ConsulGlobalMutexService(tokenSource::get, consulUrl, tokenSource::reportRefusal);
    }
}
