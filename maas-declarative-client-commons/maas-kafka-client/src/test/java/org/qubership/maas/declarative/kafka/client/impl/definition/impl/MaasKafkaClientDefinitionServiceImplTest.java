package org.qubership.maas.declarative.kafka.client.impl.definition.impl;

import org.qubership.cloud.maas.bluegreen.kafka.ConsumerConsistencyMode;
import org.qubership.cloud.maas.bluegreen.kafka.OffsetSetupStrategy;
import org.qubership.maas.declarative.kafka.client.TestUtils;
import org.qubership.maas.declarative.kafka.client.api.exception.MaasKafkaMandatoryPropertyAbsentException;
import org.qubership.maas.declarative.kafka.client.api.model.definition.ManagedBy;
import org.qubership.maas.declarative.kafka.client.impl.Utils;
import org.qubership.maas.declarative.kafka.client.impl.definition.api.MaasKafkaClientConfigPlatformService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.qubership.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants.POOL_DURATION;
import static org.qubership.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants.TOPIC_PARTITIONS;
import static org.junit.jupiter.api.Assertions.*;

class MaasKafkaClientDefinitionServiceImplTest {
    static MaasKafkaClientConfigPlatformService config = prefix ->
                Utils.prepareConfigAsMap(TestUtils.loadConfig("declaration-part.yaml"),prefix);

    @Test
    void getProducerDefinition_TopicPartitionsAsDouble() {
        var service = new MaasKafkaClientDefinitionServiceImpl(prefix -> {
            final Map<String, Object> configAsMap = Utils.prepareConfigAsMap(TestUtils.loadConfig("declaration-part.yaml"), prefix);
            configAsMap.put(TOPIC_PARTITIONS, 2.0);
            return configAsMap;
        });
        var def = service.getProducerDefinition("quota-spec-cdc-outgoing-topic");
        assertEquals(2, def.getTopic().getPartitions());
    }

    @Test
    void getProducerDefinition_TopicPartitionsAsInteger() {
        var service = new MaasKafkaClientDefinitionServiceImpl(prefix -> {
            final Map<String, Object> configAsMap = Utils.prepareConfigAsMap(TestUtils.loadConfig("declaration-part.yaml"), prefix);
            configAsMap.put(TOPIC_PARTITIONS, 2);
            return configAsMap;
        });
        var def = service.getProducerDefinition("quota-spec-cdc-outgoing-topic");
        assertEquals(2, def.getTopic().getPartitions());
    }

    @Test
    void getProducerDefinition_TopicPartitionsAsString() {
        var service = new MaasKafkaClientDefinitionServiceImpl(prefix -> {
            final Map<String, Object> configAsMap = Utils.prepareConfigAsMap(TestUtils.loadConfig("declaration-part.yaml"), prefix);
            configAsMap.put(TOPIC_PARTITIONS, "2");
            return configAsMap;
        });
        var def = service.getProducerDefinition("quota-spec-cdc-outgoing-topic");
        assertEquals(2, def.getTopic().getPartitions());
    }

    @Test
    void getConsumerDefinition_PollDurationAsDouble() {
        var service = new MaasKafkaClientDefinitionServiceImpl(prefix -> {
            final Map<String, Object> configAsMap = Utils.prepareConfigAsMap(TestUtils.loadConfig("declaration-part.yaml"), prefix);
            configAsMap.put(POOL_DURATION, 10.0);
            return configAsMap;
        });
        var def = service.getConsumerDefinition("qm-sales-lead-change");
        assertEquals(10, def.getPollDuration());
    }

    @Test
    void getConsumerDefinition_PollDurationNull() {
        var service = new MaasKafkaClientDefinitionServiceImpl(prefix -> {
            final Map<String, Object> configAsMap = Utils.prepareConfigAsMap(TestUtils.loadConfig("declaration-part.yaml"), prefix);
            configAsMap.put(POOL_DURATION, null);
            return configAsMap;
        });
        var def = service.getConsumerDefinition("qm-sales-lead-change");
        assertNull(def.getPollDuration());
    }

    @Test
    void getProducerDefinition() {
        var service = new MaasKafkaClientDefinitionServiceImpl(config);

        var def = service.getProducerDefinition("quota-spec-cdc-outgoing-topic");
        assertEquals(ManagedBy.MAAS, def.getTopic().getManagedBy());
        assertEquals("quota-spec-cdc-outgoing-topic", def.getTopic().getName());
        assertEquals("ex-quota-spec-cdc-outgoing", def.getTopic().getActualName());
        assertEquals("qm-local", def.getTopic().getNamespace());
        assertEquals("merge", def.getTopic().getOnTopicExist());
        assertEquals("base", def.getTopic().getTemplate());
        assertEquals("inherit", def.getTopic().getReplicationFactor());
        assertEquals(3, def.getTopic().getPartitions());
        assertTrue(def.isTenant());
        assertEquals(Map.of(
                "value.serializer", "io.quarkus.kafka.client.serialization.ObjectMapperSerializer",
                "key.serializer", "io.quarkus.kafka.client.serialization.ObjectMapperSerializer"
                ),
                def.getClientConfig()
            );
        assertEquals(Map.of("abc", "cde",
                "max.compaction.lag.ms", "10000"), def.getTopic().getConfigs());
    }

    @Test
    void getConsumerDefinition() {
        var service = new MaasKafkaClientDefinitionServiceImpl(config);

        var def = service.getConsumerDefinition("qm-sales-lead-change");
        assertEquals(ManagedBy.SELF, def.getTopic().getManagedBy());
        assertEquals("sales-lead-change-topic", def.getTopic().getName());
        assertEquals("qm-local-lead-management-core-service-sales-lead-change-{tenant-id}", def.getTopic().getActualName());
        assertEquals("qm-local", def.getTopic().getNamespace());
        assertTrue(def.isTenant());
        assertEquals("merge", def.getTopic().getOnTopicExist());
        assertEquals("qm-local-${cloud.application.name}-sales-lead-change-group", def.getGroupId());
        assertEquals(Map.of(
                        "key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer",
                        "value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer",
                        "auto.offset.reset", "latest",
                        "allow.auto.create.topics", "false",
                        "errors.tolerance", "all",
                        "group.id", "qm-local-${cloud.application.name}-sales-lead-change-group",
                        "enable.auto.commit", "false"
                ),
                def.getClientConfig()
        );
        assertEquals(10, def.getPollDuration());
        assertEquals(1, def.getInstanceCount());
        assertEquals(Map.of("abc", "cde",
                "max.compaction.lag.ms", "10000"), def.getTopic().getConfigs());
    }

    @Test
    void testWrongConsumerDefinition() {
        var service = new MaasKafkaClientDefinitionServiceImpl(prefix -> Map.of());
        var e = assertThrowsExactly(
                MaasKafkaMandatoryPropertyAbsentException.class,
                () -> service.getConsumerDefinition("qm-sales-lead-change")
            );

        assertEquals("Mandatory property \"maas.kafka.client.consumer.qm-sales-lead-change.topic.name\" is absent", e.getMessage());
    }

    @Test
    void getConsumerDefinition_BlueGreen() {
        var service = new MaasKafkaClientDefinitionServiceImpl(prefix -> Utils.prepareConfigAsMap(TestUtils.loadConfig("declaration-part.yaml"), prefix));
        var def = service.getConsumerDefinition("qm-sales-lead-change");
        assertTrue(def.getBlueGreenDefinition().isEnabled());
        assertFalse(def.getBlueGreenDefinition().isFilterEnabled());
        assertTrue(def.getBlueGreenDefinition().isLocaldev());
        assertEquals(ConsumerConsistencyMode.GUARANTEE_CONSUMPTION, def.getBlueGreenDefinition().getConsumerConsistencyMode());
        assertEquals(OffsetSetupStrategy.rewind(Duration.ofSeconds(5)), def.getBlueGreenDefinition().getCandidateOffsetShift());
    }

    @Test
    void getConsumerDefinition_BlueGreen_Defaults() {
        var service = new MaasKafkaClientDefinitionServiceImpl(prefix -> Utils.prepareConfigAsMap(TestUtils.loadConfig("declaration-part.yaml"), prefix));
        var def = service.getConsumerDefinition("qm-sales-lead-change-use-bg-defaults");
        assertTrue(def.getBlueGreenDefinition().isEnabled());
        assertTrue(def.getBlueGreenDefinition().isFilterEnabled());
        assertEquals(ConsumerConsistencyMode.EVENTUAL, def.getBlueGreenDefinition().getConsumerConsistencyMode());
        assertEquals(OffsetSetupStrategy.rewind(Duration.ofMinutes(5)), def.getBlueGreenDefinition().getCandidateOffsetShift());
    }

    @Test
    void getProducerDefinition_Versioned() {
        var service = new MaasKafkaClientDefinitionServiceImpl(prefix -> Utils.prepareConfigAsMap(TestUtils.loadConfig("declaration-part.yaml"), prefix));
        var def = service.getProducerDefinition("quota-spec-cdc-outgoing-topic");
        assertTrue(def.getTopic().isVersioned());
    }

    @Test
    void getConsumerDefinition_Versioned() {
        var service = new MaasKafkaClientDefinitionServiceImpl(prefix -> Utils.prepareConfigAsMap(TestUtils.loadConfig("declaration-part.yaml"), prefix));
        var def = service.getConsumerDefinition("qm-sales-lead-change");
        assertTrue(def.getTopic().isVersioned());
    }

    @Test
    void getConsumerDefinition_DedicatedThreadPoolSize() {
        var service = new MaasKafkaClientDefinitionServiceImpl(prefix -> Utils.prepareConfigAsMap(TestUtils.loadConfig("declaration-part.yaml"), prefix));
        var def = service.getConsumerDefinition("quota-cleaning-notification");
        assertNotNull(def);
        assertEquals(5, def.getDedicatedThreadPoolSize());
    }
}