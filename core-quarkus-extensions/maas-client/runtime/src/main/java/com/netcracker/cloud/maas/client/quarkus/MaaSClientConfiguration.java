package com.netcracker.cloud.maas.client.quarkus;

import com.netcracker.cloud.quarkus.security.auth.M2MManager;
import com.netcracker.cloud.maas.client.api.MaaSAPIClient;
import com.netcracker.cloud.maas.client.api.kafka.KafkaMaaSClient;
import com.netcracker.cloud.maas.client.api.rabbit.RabbitMaaSClient;
import com.netcracker.cloud.maas.client.impl.MaaSAPIClientImpl;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Dependent
public class MaaSClientConfiguration {

    @ConfigProperty(name = "security.m2m.kubernetes.enabled", defaultValue = "false")
    boolean k8sM2mEnabled;

    @Produces
    @DefaultBean
    @Singleton
    public MaaSAPIClient getMaaSAPIClient() {
        return new MaaSAPIClientImpl(() -> M2MManager.getInstance().getToken().getTokenValue(), k8sM2mEnabled);
    }

    @Produces
    @DefaultBean
    @Singleton
    public KafkaMaaSClient getKafkaMaaSClient(MaaSAPIClient client) {
        return client.getKafkaClient();
    }

    @Produces
    @DefaultBean
    @Singleton
    public RabbitMaaSClient getRabbitMaaSClient(MaaSAPIClient client) {
        return client.getRabbitClient();
    }
}
