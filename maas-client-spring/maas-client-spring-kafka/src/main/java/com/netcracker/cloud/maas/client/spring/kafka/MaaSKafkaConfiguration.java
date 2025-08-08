package org.qubership.cloud.maas.client.spring.kafka;

import org.qubership.cloud.maas.client.api.MaaSAPIClient;
import org.qubership.cloud.maas.client.api.kafka.KafkaMaaSClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MaaSKafkaConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KafkaMaaSClient kafkaMaaSClient(MaaSAPIClient client) {
        return client.getKafkaClient();
    }
}
