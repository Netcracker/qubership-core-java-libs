package org.qubership.cloud.maas.client.quarkus;

import org.qubership.cloud.maas.client.api.MaaSAPIClient;
import org.qubership.cloud.maas.client.api.kafka.KafkaMaaSClient;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Dependent
public class MaaSClientKafkaConfiguration {

    @Produces
    @DefaultBean
    @Singleton
    public KafkaMaaSClient getKafkaMaaSClient(MaaSAPIClient client) {
        return client.getKafkaClient();
    }
}
