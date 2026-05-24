package com.netcracker.cloud.maas.client.quarkus;

import com.netcracker.cloud.maas.client.api.MaaSAPIClient;
import com.netcracker.cloud.maas.client.impl.MaaSAPIClientImpl;
import com.netcracker.cloud.quarkus.security.auth.M2MManager;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Dependent
public class MaaSClientCommonConfiguration {

    @ConfigProperty(name = "security.m2m.kubernetes.enabled", defaultValue = "false")
    boolean k8sEnabled;

    @Produces
    @DefaultBean
    @Singleton
    public MaaSAPIClient getMaaSAPIClient() {
        return new MaaSAPIClientImpl(() -> M2MManager.getInstance().getToken().getTokenValue(), k8sEnabled);
    }
}
