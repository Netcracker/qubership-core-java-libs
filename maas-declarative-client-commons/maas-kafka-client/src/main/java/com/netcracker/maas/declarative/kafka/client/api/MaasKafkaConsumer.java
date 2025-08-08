package org.qubership.maas.declarative.kafka.client.api;

import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaConsumerDefinition;
import org.apache.kafka.clients.consumer.Consumer;

/**
 * Maas kafka consumer interface
 */
public interface MaasKafkaConsumer extends MaasKafkaClient {

    /**
     * Returns kafka topic, if {@link MaasKafkaConsumerDefinition#isTenant()} returns true,
     * tenant in context  will be used to get topic
     *
     * @return kafka topic
     */
    TopicAddress getTopic();

    /**
     * Returns true if consumer was created with MaasKafkaConsumerCreationRequest.isCustomProcessed() = true
     * otherwise false
     *
     * @return bool value
     */
    boolean isCustomProcessed();

    /**
     * Returns kafka consumer if {@link MaasKafkaConsumer#isCustomProcessed()} returns true
     * otherwise null (for service topic)
     *
     * @return default kafka consumer
     */
    @Deprecated
    Consumer unwrap();

    /**
     * Returns kafka consumer if {@link MaasKafkaConsumer#isCustomProcessed()} returns true
     * otherwise null (for tenant topic)
     *
     * @param tenantId string in UUID format
     * @return default kafka consumer
     */
    @Deprecated
    Consumer unwrap(String tenantId);

    /**
     * Returns definition object that has been used to create this consumer
     * @return MaasKafkaConsumerDefinition
     */
    MaasKafkaConsumerDefinition getDefinition();
}
