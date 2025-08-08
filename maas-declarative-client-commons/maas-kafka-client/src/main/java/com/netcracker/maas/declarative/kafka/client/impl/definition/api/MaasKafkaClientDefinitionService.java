package com.netcracker.maas.declarative.kafka.client.impl.definition.api;

import org.qubership.maas.declarative.kafka.client.api.exception.MaasKafkaMandatoryPropertyAbsentException;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaConsumerDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition;

/**
 * Used to extract client definitions from configuration properties
 *
 * WARNING! This is internal API that will be removed in future releases, do not use it directly
 */
public interface MaasKafkaClientDefinitionService {
    /**
     * Get producer definition from application properties by producer name
     *
     * @throws MaasKafkaMandatoryPropertyAbsentException - if mandatory property not provided in application properties
     * @param producerName producer name
     * @return instance
     */
    MaasKafkaProducerDefinition getProducerDefinition(String producerName);

    /**
     * Get consumer definition from application properties by consumer name
     * @throws MaasKafkaMandatoryPropertyAbsentException - if mandatory property not provided in application properties
     * @param consumerName consumer name
     * @return instance
     */
    MaasKafkaConsumerDefinition getConsumerDefinition(String consumerName);

}
