package com.netcracker.cloud.maas.client.quarkus;

import com.netcracker.cloud.quarkus.security.auth.M2MManager;
import com.netcracker.cloud.maas.client.api.MaaSAPIClient;
import com.netcracker.cloud.maas.client.api.kafka.KafkaMaaSClient;
import com.netcracker.cloud.maas.client.api.rabbit.RabbitMaaSClient;
import com.netcracker.cloud.maas.client.impl.MaaSAPIClientImpl;
import com.netcracker.cloud.security.core.utils.k8s.AudienceName;
import com.netcracker.cloud.security.core.utils.k8s.KubernetesAudienceToken;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Dependent
public class MaaSClientConfiguration {
    @Produces
    @DefaultBean
    @Singleton
    public MaaSAPIClient getMaaSAPIClient() {
        return new MaaSAPIClientImpl(() -> KubernetesAudienceToken.getToken(AudienceName.NETCRACKER));
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
