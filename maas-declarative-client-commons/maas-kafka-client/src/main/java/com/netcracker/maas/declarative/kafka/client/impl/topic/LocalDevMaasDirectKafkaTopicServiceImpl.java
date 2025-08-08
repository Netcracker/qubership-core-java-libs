package com.netcracker.maas.declarative.kafka.client.impl.topic;

import org.qubership.cloud.maas.client.api.Classifier;
import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.cloud.maas.client.api.kafka.TopicUserCredentials;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaCommonClientDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasTopicDefinition;
import org.qubership.maas.declarative.kafka.client.impl.local.dev.config.api.MaasKafkaLocalDevConfigProviderService;
import lombok.Value;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;

import java.util.*;

import static org.qubership.cloud.maas.client.api.Classifier.NAMESPACE;


// for development
public class LocalDevMaasDirectKafkaTopicServiceImpl implements MaasKafkaTopicService {

    private final Map<String, Object> kafkaConfig;

    private static final String TENANT_ID_PLACEHOLDER = "{tenant-id}";
    private static final String NAMESPACE_PLACEHOLDER = "{namespace}";

    public LocalDevMaasDirectKafkaTopicServiceImpl(MaasKafkaLocalDevConfigProviderService maasKafkaConfigProviderService) {
        this.kafkaConfig = maasKafkaConfigProviderService.get();
    }

    private TopicAddress produce(MaasKafkaCommonClientDefinition clientDefinition, String tenantId) {
        Classifier classifier = new Classifier(clientDefinition.getTopic().getName(), NAMESPACE, clientDefinition.getTopic().getNamespace());
        if (tenantId != null) {
            classifier = classifier.tenantId(tenantId);
        }
        return new LocalDevTopicAddress(
                classifier,
                tenantId == null ? getFullyQualifiedTopicAddressName(clientDefinition) : getFullyQualifiedTopicAddressNameByTenantId(clientDefinition, tenantId),
                clientDefinition.getTopic().getPartitions() == null ? 1 : clientDefinition.getTopic().getPartitions(),
                kafkaConfig
            );
        }

    @Override
    public TopicAddress getTopicAddressByDefinition(MaasKafkaCommonClientDefinition clientDefinition) {
        return getOrCreateByDefinition(clientDefinition, null, false);
    }

    @Override
    public TopicAddress getTopicAddressByDefinitionAndTenantId(MaasKafkaCommonClientDefinition clientDefinition, String tenantId) {
        return getOrCreateByDefinition(clientDefinition, tenantId, false);
    }

    @Override
    public TopicAddress getOrCreateTopicAddressByDefinition(MaasKafkaCommonClientDefinition clientDefinition) {
        return getOrCreateByDefinition(clientDefinition, null, true);
    }

    @Override
    public TopicAddress getOrCreateTopicAddressByDefinitionAndTenantId(MaasKafkaCommonClientDefinition clientDefinition, String tenantId) {
        return getOrCreateByDefinition(clientDefinition, tenantId, true);
    }

    private TopicAddress getOrCreateByDefinition(MaasKafkaCommonClientDefinition clientDefinition, String tenantId, boolean create) {
        final TopicAddress topicAddress = produce(clientDefinition, tenantId);
        if (topicExists(topicAddress)) {
            return topicAddress;
        }
        if (create) {
            return getOrCreateTopic(topicAddress);
        }
        return null;
    }

    @Override
    public String getFullyQualifiedTopicAddressName(MaasKafkaCommonClientDefinition clientDefinition) {
        MaasTopicDefinition topicDefinition = clientDefinition.getTopic();
        if (topicDefinition.getActualName() != null) {
            String actualName = topicDefinition.getActualName();

            if (actualName.contains(NAMESPACE_PLACEHOLDER)) {
                actualName = actualName.replace(NAMESPACE_PLACEHOLDER, topicDefinition.getNamespace());
            }
            return actualName;
        }
        return topicDefinition.getName();
    }

    @Override
    public String getFullyQualifiedTopicAddressNameByTenantId(MaasKafkaCommonClientDefinition clientDefinition, String tenantId) {
        MaasTopicDefinition topicDefinition = clientDefinition.getTopic();
        if (topicDefinition.getActualName() != null) {
            String actualName = topicDefinition.getActualName();
            if (actualName.contains(TENANT_ID_PLACEHOLDER)) {
                actualName = actualName.replace(TENANT_ID_PLACEHOLDER, tenantId);
            }
            if (actualName.contains(NAMESPACE_PLACEHOLDER)) {
                actualName = actualName.replace(NAMESPACE_PLACEHOLDER, topicDefinition.getNamespace());
            }
            return actualName;
        }
        return topicDefinition.getName();
    }

    protected TopicAddress getOrCreateTopic(TopicAddress topic) {
        try (Admin admin = Admin.create(kafkaConfig)) {
            KafkaFuture<Set<String>> listTopics = admin.listTopics().names();
            Set<String> topicNamesSet = listTopics.get();
            if (topicNamesSet.contains(topic.getTopicName())) {
                return topic;
            }

            NewTopic newTopic = new NewTopic(
                    topic.getTopicName(),
                    Optional.of(topic.getNumPartitions()),
                    Optional.of((short) -1));
            admin.createTopics(Collections.singleton(newTopic)).all().get();
            return topic;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected boolean topicExists(TopicAddress topic) {
        try (Admin admin = Admin.create(kafkaConfig)) {
            KafkaFuture<Set<String>> listTopics = admin.listTopics().names();
            Set<String> topicNamesSet = listTopics.get();
            return topicNamesSet.contains(topic.getTopicName());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Value
    class LocalDevTopicAddress implements TopicAddress {
        Classifier classifier;
        String topicName;
        String CACert = null;
        int numPartitions;
        Map<String, Object> kafkaConfig;

        public String getBoostrapServers(String protocol){
            throw new RuntimeException("Not implemented for LocalDev");
        }

        public Optional<TopicUserCredentials> getCredentials(String protocol){
            throw new RuntimeException("Not implemented for LocalDev");
        }

        public Optional<Map<String, Object>> formatConnectionProperties(){
            return Optional.of(kafkaConfig);
        }

        @Override
        public boolean isVersioned() {
            return false;
        }
    }
}
