package org.qubership.maas.declarative.kafka.client.impl.topic;

import org.qubership.cloud.maas.client.api.Classifier;
import org.qubership.cloud.maas.client.api.kafka.KafkaMaaSClient;
import org.qubership.cloud.maas.client.api.kafka.TopicCreateOptions;
import org.qubership.cloud.maas.client.api.kafka.protocolextractors.OnTopicExists;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaCommonClientDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaConsumerDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasTopicDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.ManagedBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MaasKafkaTopicServiceImplTest {

    private static MaasKafkaTopicServiceImpl service;
    private static KafkaMaaSClient client;

    public static final String TEST_TOPIC_NAME = "test-topic";
    public static final String TEST_NAMESPACE_NAME = "test-namespace";
    public static final String TEST_GROUP_ID_NAME = "test-groupId";
    public static final String TENANT_ID = "test";

    @BeforeEach
    void setUp() {
        client = mock(KafkaMaaSClient.class);
        service = new MaasKafkaTopicServiceImpl(client);
    }

    @Test
    void buildTopicCreateOptions() {
        var service = new MaasKafkaTopicServiceImpl(null);
        var def = MaasTopicDefinition.builder()
                .setActualName("orders_{tenant-id}_{namespace}")
                .setName("orders")
                .setNamespace("core-dev")
                .setManagedBy(ManagedBy.MAAS)
                .setOnTopicExist("merge")
                .setConfigs(Map.of(
                                "flush.ms", 1000,
                                "other", "any"
                        )
                ).build();

        var options = service.buildTopicCreateOptions(def, "default");

        var expected = TopicCreateOptions.builder()
                .name("orders_default_core-dev")
                .onTopicExists(OnTopicExists.MERGE)
                .configs(Map.of(
                        "flush.ms", "1000",
                        "other", "any"
                )).build();
        assertEquals(expected, options);
    }

    @Test
    void getTopicAddressByDefinition() {
        MaasKafkaCommonClientDefinition clientDefinition = MaasKafkaConsumerDefinition.builder()
                .setTopic(MaasTopicDefinition.builder()
                        .setName(TEST_TOPIC_NAME)
                        .setNamespace(TEST_NAMESPACE_NAME)
                        .setManagedBy(ManagedBy.SELF)
                        .build())
                .setGroupId(TEST_GROUP_ID_NAME)
                .build();

        service.getTopicAddressByDefinition(clientDefinition);
        ArgumentCaptor<Classifier> classifierCaptor = ArgumentCaptor.forClass(Classifier.class);
        verify(client).getTopic(classifierCaptor.capture());
        assertEquals(TEST_NAMESPACE_NAME, classifierCaptor.getValue().toMap().get("namespace"));
        assertEquals(TEST_TOPIC_NAME, classifierCaptor.getValue().toMap().get("name"));
    }

    @Test
    void getTopicAddressByDefinitionAndTenantId() {
        MaasKafkaCommonClientDefinition clientDefinition = MaasKafkaConsumerDefinition.builder()
                .setTopic(MaasTopicDefinition.builder()
                        .setName(TEST_TOPIC_NAME)
                        .setNamespace(TEST_NAMESPACE_NAME)
                        .setManagedBy(ManagedBy.SELF)
                        .build())
                .setGroupId(TEST_GROUP_ID_NAME)
                .build();

        service.getTopicAddressByDefinitionAndTenantId(clientDefinition, TENANT_ID);
        ArgumentCaptor<Classifier> classifierCaptor = ArgumentCaptor.forClass(Classifier.class);
        verify(client).getTopic(classifierCaptor.capture());
        assertEquals(TEST_NAMESPACE_NAME, classifierCaptor.getValue().toMap().get("namespace"));
        assertEquals(TEST_TOPIC_NAME, classifierCaptor.getValue().toMap().get("name"));
        assertEquals(TENANT_ID, classifierCaptor.getValue().toMap().get("tenantId"));
    }

    @Test
    void getOrCreateTopicAddressByDefinition() {
        MaasKafkaCommonClientDefinition clientDefinition = MaasKafkaConsumerDefinition.builder()
                .setTopic(MaasTopicDefinition.builder()
                        .setName(TEST_TOPIC_NAME)
                        .setNamespace(TEST_NAMESPACE_NAME)
                        .setManagedBy(ManagedBy.SELF)
                        .setPartitions(24)
                        .setReplicationFactor("42")
                        .setTemplate("test-template")
                        .build())
                .setGroupId(TEST_GROUP_ID_NAME)
                .build();

        service.getOrCreateTopicAddressByDefinition(clientDefinition);
        ArgumentCaptor<Classifier> classifierCaptor = ArgumentCaptor.forClass(Classifier.class);
        ArgumentCaptor<TopicCreateOptions> topicCaptor = ArgumentCaptor.forClass(TopicCreateOptions.class);
        verify(client).getOrCreateTopic(classifierCaptor.capture(), topicCaptor.capture());
        assertEquals(TEST_NAMESPACE_NAME, classifierCaptor.getValue().toMap().get("namespace"));
        assertEquals(TEST_TOPIC_NAME, classifierCaptor.getValue().toMap().get("name"));
        assertEquals(24, topicCaptor.getValue().getNumPartitions());
        assertEquals("42", topicCaptor.getValue().getReplicationFactor());
        assertEquals("test-template", topicCaptor.getValue().getTemplate());
    }

    @Test
    void getOrCreateTopicAddressByDefinitionAndTenantId() {
        MaasKafkaCommonClientDefinition clientDefinition = MaasKafkaConsumerDefinition.builder()
                .setTopic(MaasTopicDefinition.builder()
                        .setName(TEST_TOPIC_NAME)
                        .setNamespace(TEST_NAMESPACE_NAME)
                        .setManagedBy(ManagedBy.SELF)
                        .setPartitions(24)
                        .setReplicationFactor("42")
                        .setTemplate("test-template")
                        .build())
                .setGroupId(TEST_GROUP_ID_NAME)
                .build();

        service.getOrCreateTopicAddressByDefinitionAndTenantId(clientDefinition, TENANT_ID);
        ArgumentCaptor<Classifier> classifierCaptor = ArgumentCaptor.forClass(Classifier.class);
        ArgumentCaptor<TopicCreateOptions> topicCaptor = ArgumentCaptor.forClass(TopicCreateOptions.class);
        verify(client).getOrCreateTopic(classifierCaptor.capture(), topicCaptor.capture());
        assertEquals(TEST_NAMESPACE_NAME, classifierCaptor.getValue().toMap().get("namespace"));
        assertEquals(TEST_TOPIC_NAME, classifierCaptor.getValue().toMap().get("name"));
        assertEquals(TENANT_ID, classifierCaptor.getValue().toMap().get("tenantId"));
        assertEquals(24, topicCaptor.getValue().getNumPartitions());
        assertEquals("42", topicCaptor.getValue().getReplicationFactor());
        assertEquals("test-template", topicCaptor.getValue().getTemplate());
    }
}