package org.qubership.maas.declarative.kafka.client.api;


import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaCommonClientDefinition;

/**
 * Interface that defines API to perform operations on kafka topics
 */
public interface MaasKafkaTopicService {

    /**
     * Get topic information
     *
     * @param clientDefinition defined client config
     * @return kafka topic
     */
    TopicAddress getTopicAddressByDefinition(MaasKafkaCommonClientDefinition clientDefinition);

    /**
     * Get topic information
     *
     * @param clientDefinition defined client config
     * @param tenantId         id of the tenant topic belongs to
     * @return kafka topic
     */
    TopicAddress getTopicAddressByDefinitionAndTenantId(MaasKafkaCommonClientDefinition clientDefinition, String tenantId);

    /**
     * Create topic
     * @param clientDefinition client definition
     * @return topic address instance
     */
    TopicAddress getOrCreateTopicAddressByDefinition(MaasKafkaCommonClientDefinition clientDefinition);

    /**
     * Create topic
     * @param clientDefinition client definition
     * @param tenantId tenant identifier as string
     * @return topic address instance
     */
    TopicAddress getOrCreateTopicAddressByDefinitionAndTenantId(MaasKafkaCommonClientDefinition clientDefinition, String tenantId);

    /**
     * Returns Maas internal fully qualified name of the topic
     *
     * @param clientDefinition defined client config
     * @return fully qualified topic name
     */
    String getFullyQualifiedTopicAddressName(MaasKafkaCommonClientDefinition clientDefinition);

    /**
     * Returns Maas internal fully qualified name of the topic
     *
     * @param clientDefinition defined client config
     * @param tenantId         id of the tenant topic belongs to
     * @return fully qualified topic name
     */
    String getFullyQualifiedTopicAddressNameByTenantId(MaasKafkaCommonClientDefinition clientDefinition, String tenantId);

}
