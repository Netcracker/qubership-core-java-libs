package com.netcracker.maas.declarative.kafka.client.api;


import com.netcracker.maas.declarative.kafka.client.api.model.MaasKafkaConsumerCreationRequest;
import com.netcracker.maas.declarative.kafka.client.api.model.MaasKafkaProducerCreationRequest;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasKafkaConsumerDefinition;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition;

/**
 * Used to get client definition from config and create kafka clients
 */
public interface MaasKafkaClientFactory {

    // TODO propagate exceptions from MaasKafkaClientDefinitionService
    /**
     * @param consumerName name should be the same as in config
     * @return consumer definition
     */
    MaasKafkaConsumerDefinition getConsumerDefinition(String consumerName);

    /**
     * @param producerName name should be the same as in config
     * @return producer definition
     */
    MaasKafkaProducerDefinition getProducerDefinition(String producerName);

    /**
     * Creates consumer with records processing method
     *
     * @param consumerCreationRequest creation request with all necessary parameters
     * @return maas kafka consumer
     */
    MaasKafkaConsumer createConsumer(MaasKafkaConsumerCreationRequest consumerCreationRequest);

    /**
     * @param producerCreationRequest creation request with all necessary parameters
     * @return maas kafka producer
     */
    MaasKafkaProducer createProducer(MaasKafkaProducerCreationRequest producerCreationRequest);

}
