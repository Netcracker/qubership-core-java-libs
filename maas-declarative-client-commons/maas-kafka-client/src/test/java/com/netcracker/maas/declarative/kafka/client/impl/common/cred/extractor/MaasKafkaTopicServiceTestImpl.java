package com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.cloud.maas.client.impl.dto.kafka.v1.TopicInfo;
import org.qubership.cloud.maas.client.impl.kafka.TopicAddressImpl;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaCommonClientDefinition;

import java.io.IOException;

public class MaasKafkaTopicServiceTestImpl implements MaasKafkaTopicService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public TopicAddress getTopicAddressByDefinition(MaasKafkaCommonClientDefinition clientDefinition) {
        try {
            return new TopicAddressImpl(
                    objectMapper
                            .readValue(
                                    getClass().getResourceAsStream(clientDefinition.getTopic().getName()),
                                    TopicInfo.class
                            )
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TopicAddress getTopicAddressByDefinitionAndTenantId(MaasKafkaCommonClientDefinition clientDefinition, String tenantId) {
        try {
            return new TopicAddressImpl(
                    objectMapper
                            .readValue(
                                    getClass().getResourceAsStream(clientDefinition.getTopic().getName()),
                                    TopicInfo.class
                            )
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TopicAddress getOrCreateTopicAddressByDefinition(MaasKafkaCommonClientDefinition clientDefinition) {
        try {
            return new TopicAddressImpl(
                    objectMapper
                            .readValue(
                                    getClass().getResourceAsStream(clientDefinition.getTopic().getName()),
                                    TopicInfo.class
                            )
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TopicAddress getOrCreateTopicAddressByDefinitionAndTenantId(MaasKafkaCommonClientDefinition clientDefinition, String tenantId) {
        try {
            return new TopicAddressImpl(
                    objectMapper
                            .readValue(
                                    getClass().getResourceAsStream(clientDefinition.getTopic().getName()),
                                    TopicInfo.class
                            )
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFullyQualifiedTopicAddressName(MaasKafkaCommonClientDefinition clientDefinition) {
        return null;
    }

    @Override
    public String getFullyQualifiedTopicAddressNameByTenantId(MaasKafkaCommonClientDefinition clientDefinition, String tenantId) {
        return null;
    }
}
