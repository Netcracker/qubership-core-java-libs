package com.netcracker.maas.declarative.kafka.client.impl.topic;

import org.qubership.cloud.maas.client.api.Classifier;
import org.qubership.cloud.maas.client.api.kafka.KafkaMaaSClient;
import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.cloud.maas.client.api.kafka.TopicCreateOptions;
import org.qubership.cloud.maas.client.api.kafka.protocolextractors.OnTopicExists;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaCommonClientDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasTopicDefinition;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


public class MaasKafkaTopicServiceImpl implements MaasKafkaTopicService {

    private final KafkaMaaSClient maasKafkaClient;

    public MaasKafkaTopicServiceImpl(KafkaMaaSClient maasKafkaClient) {
        this.maasKafkaClient = maasKafkaClient;
    }

    @Override
    public TopicAddress getTopicAddressByDefinition(MaasKafkaCommonClientDefinition clientDefinition) {
        return maasKafkaClient.getTopic(toClassifier(clientDefinition)).orElse(null);
    }

    @Override
    public TopicAddress getTopicAddressByDefinitionAndTenantId(MaasKafkaCommonClientDefinition clientDefinition, String tenantId) {
        return maasKafkaClient.getTopic(toClassifier(clientDefinition).tenantId(tenantId)).orElse(null);
    }

    @Override
    public TopicAddress getOrCreateTopicAddressByDefinition(MaasKafkaCommonClientDefinition clientDefinition) {
        return maasKafkaClient.getOrCreateTopic(
                toClassifier(clientDefinition),
                buildTopicCreateOptions(clientDefinition.getTopic(), null)
            );
    }

    @Override
    public TopicAddress getOrCreateTopicAddressByDefinitionAndTenantId(MaasKafkaCommonClientDefinition clientDefinition, String tenantId) {
        return maasKafkaClient.getOrCreateTopic(
                toClassifier(clientDefinition).tenantId(tenantId),
                buildTopicCreateOptions(clientDefinition.getTopic(), tenantId)
            );
    }

    @Override
    public String getFullyQualifiedTopicAddressName(MaasKafkaCommonClientDefinition clientDefinition) {
        return maasKafkaClient.getTopic(toClassifier(clientDefinition))
                .map(TopicAddress::getTopicName)
                .orElse(null);
    }

    @Override
    public String getFullyQualifiedTopicAddressNameByTenantId(MaasKafkaCommonClientDefinition clientDefinition, String tenantId) {
        return maasKafkaClient.getTopic(toClassifier(clientDefinition).tenantId(tenantId))
                .map(TopicAddress::getTopicName)
                .orElse(null);
    }

    private Classifier toClassifier(MaasKafkaCommonClientDefinition def) {
        return new Classifier(def.getTopic().getName(), Classifier.NAMESPACE, def.getTopic().getNamespace());
    }

    TopicCreateOptions buildTopicCreateOptions(MaasTopicDefinition def, String tenantId) {
        TopicCreateOptions.TopicCreateOptionsBuilder builder = TopicCreateOptions.builder();

        // TODO deprecate this name templating feature in favor of builtin name templates in maas
        Optional.ofNullable(def.getActualName()).ifPresent(actualName -> {
            if (tenantId != null) {
                actualName = actualName.replace("{tenant-id}", tenantId);
            }
            actualName = actualName.replace("{namespace}", def.getNamespace());
            builder.name(actualName);
        });

        Optional.ofNullable(def.getPartitions()).ifPresent(builder::numPartitions);
        Optional.ofNullable(def.getReplicationFactor()).ifPresent(value -> {
                        if ("inherit".equals(value)) {
                            builder.inheritReplicationFactor();
                        } else {
                            builder.replicationFactor(Integer.parseInt(value));
                        }
                    });
        Optional.ofNullable(def.getTemplate()).ifPresent(builder::template);
        Optional.ofNullable(def.getOnTopicExist())
                .map(String::toUpperCase)
                .map(OnTopicExists::valueOf)
                .ifPresent(builder::onTopicExists);
        builder.versioned(def.isVersioned());

        if (def.getConfigs() != null) {
            builder.configs(
                    // TODO review why we use Map<String, Object> instead Map<String, String>?
                    def.getConfigs()
                            .entrySet()
                            .stream()
                            .collect(
                                    Collectors.toMap(
                                            Map.Entry::getKey,
                                            e -> String.valueOf(e.getValue())
                                    )
                            )
            );
        }

        return builder.build();
    }
}
