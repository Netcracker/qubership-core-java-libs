package com.netcracker.maas.declarative.kafka.client.impl.topic;

import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaCommonClientDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaConsumerDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasTopicDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.ManagedBy;
import org.qubership.maas.declarative.kafka.client.impl.local.dev.config.api.MaasKafkaLocalDevConfigProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LocalDevMaasDirectKafkaTopicServiceImplTest {

    public static final String TEST_TOPIC_NAME = "test-topic";
    public static final String TEST_NAMESPACE_NAME = "test-namespace";
    public static final String TEST_GROUP_ID_NAME = "test-groupId";
    public static final String TENANT_ID = "test";
    private static LocalDevMaasDirectKafkaTopicServiceImpl service;

    @BeforeEach
    void setUp() {
        MaasKafkaLocalDevConfigProviderService localDevConfigProviderService = mock(MaasKafkaLocalDevConfigProviderService.class);
        service = spy(new LocalDevMaasDirectKafkaTopicServiceImpl(localDevConfigProviderService));
    }

    @Test
    void getTopicAddressByDefinition() {
        doReturn(true).when(service).topicExists(any());

        MaasKafkaCommonClientDefinition clientDefinition = MaasKafkaConsumerDefinition.builder()
                .setTopic(MaasTopicDefinition.builder()
                        .setName(TEST_TOPIC_NAME)
                        .setNamespace(TEST_NAMESPACE_NAME)
                        .setManagedBy(ManagedBy.SELF)
                        .build())
                .setGroupId(TEST_GROUP_ID_NAME)
                .build();

        TopicAddress address = service.getTopicAddressByDefinition(clientDefinition);
        assertEquals(TEST_TOPIC_NAME, address.getTopicName());
    }

    @Test
    void getTopicAddressByDefinitionAndTenantId() {
        doReturn(true).when(service).topicExists(any());

        MaasKafkaCommonClientDefinition clientDefinition = MaasKafkaConsumerDefinition.builder()
                .setTopic(MaasTopicDefinition.builder()
                        .setName(TEST_TOPIC_NAME)
                        .setNamespace(TEST_NAMESPACE_NAME)
                        .setManagedBy(ManagedBy.SELF)
                        .setActualName(TEST_TOPIC_NAME + "_{namespace}_{tenant-id}")
                        .build())
                .setGroupId(TEST_GROUP_ID_NAME)
                .build();
        TopicAddress address = service.getTopicAddressByDefinitionAndTenantId(clientDefinition, TENANT_ID);
        assertEquals(TEST_TOPIC_NAME + "_" + TEST_NAMESPACE_NAME + "_" + TENANT_ID, address.getTopicName());

    }

    @Test
    void getOrCreateTopicAddressByDefinition() {
        MaasKafkaCommonClientDefinition clientDefinition = MaasKafkaConsumerDefinition.builder()
                .setTopic(MaasTopicDefinition.builder()
                        .setName(TEST_TOPIC_NAME)
                        .setNamespace(TEST_NAMESPACE_NAME)
                        .setManagedBy(ManagedBy.SELF)
                        .setActualName(TEST_TOPIC_NAME + "_{namespace}")
                        .setReplicationFactor("42")
                        .setPartitions(24)
                        .build())
                .setGroupId(TEST_GROUP_ID_NAME)
                .build();

        doReturn(false).when(service).topicExists(any());
        doReturn(mock(TopicAddress.class)).when(service).getOrCreateTopic(any());

        service.getOrCreateTopicAddressByDefinition(clientDefinition);
        ArgumentCaptor<TopicAddress> topicInfoCaptor = ArgumentCaptor.forClass(TopicAddress.class);
        verify(service).getOrCreateTopic(topicInfoCaptor.capture());
        TopicAddress topicInfoCaptorValue = topicInfoCaptor.getValue();

        assertEquals(TEST_TOPIC_NAME + "_" + TEST_NAMESPACE_NAME, topicInfoCaptorValue.getTopicName());
        assertEquals(TEST_NAMESPACE_NAME, topicInfoCaptorValue.getClassifier().getNamespace());
        assertEquals(24, topicInfoCaptorValue.getNumPartitions());
    }

    @Test
    void getOrCreateTopicAddressByDefinitionAndTenantId() {
        MaasKafkaCommonClientDefinition clientDefinition = MaasKafkaConsumerDefinition.builder()
                .setTopic(MaasTopicDefinition.builder()
                        .setName(TEST_TOPIC_NAME)
                        .setNamespace(TEST_NAMESPACE_NAME)
                        .setManagedBy(ManagedBy.SELF)
                        .setActualName(TEST_TOPIC_NAME + "_{namespace}_{tenant-id}")
                        .setReplicationFactor("42")
                        .setPartitions(24)
                        .build())
                .setGroupId(TEST_GROUP_ID_NAME)
                .build();

        ArgumentCaptor<TopicAddress> topicInfoCaptor = ArgumentCaptor.forClass(TopicAddress.class);
        doReturn(false).when(service).topicExists(any());
        doReturn(mock(TopicAddress.class)).when(service).getOrCreateTopic(any());

        service.getOrCreateTopicAddressByDefinitionAndTenantId(clientDefinition, TENANT_ID);
        verify(service).getOrCreateTopic(topicInfoCaptor.capture());
        TopicAddress topicInfoCaptorValue = topicInfoCaptor.getValue();

        assertEquals(TEST_TOPIC_NAME + "_" + TEST_NAMESPACE_NAME + "_" + TENANT_ID, topicInfoCaptorValue.getTopicName());
        assertEquals(TEST_NAMESPACE_NAME, topicInfoCaptorValue.getClassifier().getNamespace());
        assertEquals(24, topicInfoCaptorValue.getNumPartitions());

    }

    @Test
    void getFullyQualifiedTopicAddressName() {
        MaasKafkaCommonClientDefinition clientDefinition = MaasKafkaConsumerDefinition.builder()
                .setTopic(MaasTopicDefinition.builder()
                        .setName(TEST_TOPIC_NAME)
                        .setNamespace(TEST_NAMESPACE_NAME)
                        .setManagedBy(ManagedBy.SELF)
                        .setActualName(TEST_TOPIC_NAME + "_{namespace}")
                        .build())
                .setGroupId(TEST_GROUP_ID_NAME)
                .build();
        String topicAddressName = service.getFullyQualifiedTopicAddressName(clientDefinition);
        assertEquals(TEST_TOPIC_NAME + "_" + TEST_NAMESPACE_NAME, topicAddressName);
    }

    @Test
    void getFullyQualifiedTopicAddressNameByTenantId() {
        MaasKafkaCommonClientDefinition clientDefinition = MaasKafkaConsumerDefinition.builder()
                .setTopic(MaasTopicDefinition.builder()
                        .setName(TEST_TOPIC_NAME)
                        .setNamespace(TEST_NAMESPACE_NAME)
                        .setManagedBy(ManagedBy.SELF)
                        .setActualName(TEST_TOPIC_NAME + "_{namespace}_{tenant-id}")
                        .build())
                .setGroupId(TEST_GROUP_ID_NAME)
                .build();
        String topicAddressName = service.getFullyQualifiedTopicAddressNameByTenantId(clientDefinition, TENANT_ID);
        assertEquals(TEST_TOPIC_NAME + "_" + TEST_NAMESPACE_NAME + "_" + TENANT_ID, topicAddressName);

    }
}
