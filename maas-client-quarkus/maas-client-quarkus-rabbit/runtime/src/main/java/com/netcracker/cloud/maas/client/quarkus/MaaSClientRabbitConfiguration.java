package org.qubership.cloud.maas.client.quarkus;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.maas.client.api.MaaSAPIClient;
import org.qubership.cloud.maas.client.api.rabbit.RabbitMaaSClient;
import org.qubership.cloud.maas.client.bluegreen.rabbit.DynamicQueueBindingsManager;
import org.qubership.cloud.maas.client.bluegreen.rabbit.DynamicQueueBindingsManagerImpl;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Dependent
public class MaaSClientRabbitConfiguration {

    @Produces
    @DefaultBean
    @Singleton
    public RabbitMaaSClient getRabbitMaaSClient(MaaSAPIClient client) {
        return client.getRabbitClient();
    }

    @Produces
    @DefaultBean
    @Singleton
    public DynamicQueueBindingsManager getDynamicQueueBindingsManager(BlueGreenStatePublisher blueGreenStatePublisher) {
        return new DynamicQueueBindingsManagerImpl(blueGreenStatePublisher);
    }
}
