package com.netcracker.maas.declarative.kafka.client.impl.definition.impl;

import com.netcracker.cloud.maas.bluegreen.kafka.ConsumerConsistencyMode;
import com.netcracker.cloud.maas.bluegreen.kafka.OffsetSetupStrategy;
import com.netcracker.maas.declarative.kafka.client.api.exception.MaasKafkaMandatoryPropertyAbsentException;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.*;
import com.netcracker.maas.declarative.kafka.client.impl.definition.api.MaasKafkaClientConfigPlatformService;
import com.netcracker.maas.declarative.kafka.client.impl.definition.api.MaasKafkaClientDefinitionService;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.netcracker.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants.*;

public class MaasKafkaClientDefinitionServiceImpl implements MaasKafkaClientDefinitionService {

    private static final String ERROR_MESSAGE_TEMPLATE = "Mandatory property \"%s\" is absent";

    private final MaasKafkaClientConfigPlatformService config;

    public MaasKafkaClientDefinitionServiceImpl(MaasKafkaClientConfigPlatformService config) {
        this.config = config;
    }

    @Override
    public MaasKafkaProducerDefinition getProducerDefinition(String producerName) {
        Map<String, Object> configMap = config.getClientConfigByPrefix(PRODUCER_PREFIX + producerName + ".");

        return MaasKafkaProducerDefinition.builder()
                .setTopic(extractTopicDefinition(configMap, PRODUCER_PREFIX + producerName))
                .setClientConfig(extractConfigSubset(configMap, KAFKA_PRODUCER_PREFIX))
                .setTenant(
                        Optional.ofNullable(configMap.get(IS_TENANT))
                                .map(Object::toString)
                                .map(Boolean::valueOf)
                                .orElse(false)
                ).build();
    }

    @Override
    public MaasKafkaConsumerDefinition getConsumerDefinition(String consumerName) {
        boolean isLocalDev = Optional.ofNullable(config.getClientConfigByPrefix(KAFKA_LOCAL_DEV_PREFIX))
                .map(localdev -> localdev.get("enabled"))
                .map(Object::toString)
                .map(Boolean::valueOf)
                .orElse(false);

        Map<String, Object> configMap = config.getClientConfigByPrefix(CONSUMER_PREFIX + consumerName + ".");

        Map<String, Object> kafkaConsumerConfig = extractConfigSubset(configMap, KAFKA_CONSUMER_PREFIX);
        // disable auto committing
        kafkaConsumerConfig.putIfAbsent("enable.auto.commit", "false");

        MaasTopicDefinition maasTopicDefinition = extractTopicDefinition(configMap, CONSUMER_PREFIX + consumerName);

        return MaasKafkaConsumerDefinition.builder()
                .setTopic(maasTopicDefinition)
                .setTenant(
                        Optional.ofNullable(configMap.get(IS_TENANT))
                                .map(Object::toString)
                                .map(Boolean::valueOf)
                                .orElse(false))
                .setClientConfig(kafkaConsumerConfig)
                .setInstanceCount(
                        Optional.ofNullable(configMap.get(INSTANCE_COUNT))
                                .map(this::parseInt)
                                .orElse(1))
                .setPollDuration(
                        Optional.ofNullable(configMap.get(POOL_DURATION))
                                .map(this::parseInt)
                                .orElse(null))
                .setGroupId(
                        Optional.ofNullable(configMap.get(GROUP_ID))
                                .map(Object::toString)
                                .orElseThrow(() -> new MaasKafkaMandatoryPropertyAbsentException(
                                        ERROR_MESSAGE_TEMPLATE, CONSUMER_PREFIX + consumerName + "." + GROUP_ID
                                )))
                .setBlueGreenDefinition(extractBlueGreenDefinition(configMap, isLocalDev))
                .setDedicatedThreadPoolSize(
                        Optional.ofNullable(configMap.get(DEDICATED_THREAD_POOL_SIZE))
                                .map(this::parseInt)
                                .orElse(null))
                .build();
    }

    private MaasKafkaBlueGreenDefinition extractBlueGreenDefinition(Map<String, Object> config, boolean isLocaldev) {
        Map<String, Object> blueGreenConfig = extractConfigSubset(config, KAFKA_CONSUMER_BLUE_GREEN_PREFIX);
        return new MaasKafkaBlueGreenDefinition(
                Optional.ofNullable(blueGreenConfig.get("enabled"))
                        .map(Object::toString)
                        .map(Boolean::valueOf)
                        .orElse(false),
                Optional.ofNullable(blueGreenConfig.get("consistency-mode"))
                        .map(Object::toString)
                        .map(ConsumerConsistencyMode::valueOf)
                        .orElse(ConsumerConsistencyMode.EVENTUAL),
                Optional.ofNullable(blueGreenConfig.get("candidate-offset-shift"))
                        .map(Object::toString)
                        .map(OffsetSetupStrategy::valueOf)
                        .orElse(OffsetSetupStrategy.rewind(Duration.ofMinutes(5))),
                Optional.ofNullable(blueGreenConfig.get("filter-enabled"))
                        .map(Object::toString)
                        .map(Boolean::valueOf)
                        .orElse(true),
                isLocaldev
        );
    }

    private MaasTopicDefinition extractTopicDefinition(Map<String, Object> config, String prefix) {
        MaasTopicDefinition.Builder builder = MaasTopicDefinition.builder();

        Function<String, Runnable> raiseException = key -> () -> {
            throw new MaasKafkaMandatoryPropertyAbsentException(ERROR_MESSAGE_TEMPLATE, prefix + "." + key);
        };

        Optional.ofNullable(config.get(TOPIC_NAME))
                .map(Object::toString)
                .ifPresentOrElse(
                        builder::setName,
                        raiseException.apply(TOPIC_NAME)
                );

        Optional.ofNullable(config.get(TOPIC_NAMESPACE))
                .map(Object::toString)
                .ifPresentOrElse(builder::setNamespace, raiseException.apply(TOPIC_NAMESPACE));

        Optional.ofNullable(config.get(TOPIC_MANAGED_BY))
                .map(Object::toString)
                .map(String::toUpperCase)
                .map(ManagedBy::valueOf)
                .ifPresentOrElse(builder::setManagedBy, raiseException.apply(TOPIC_MANAGED_BY));

        Optional.ofNullable(config.get(TOPIC_VERSIONED))
                .map(Object::toString)
                .map(Boolean::valueOf)
                .ifPresent(builder::setVersioned);

        Optional.ofNullable(config.get(TOPIC_ACTUAL_NAME))
                .map(Object::toString)
                .ifPresent(builder::setActualName);

        Optional.ofNullable(config.get(TOPIC_TEMPLATE))
                .map(Object::toString)
                .ifPresent(builder::setTemplate);

        Optional.ofNullable(config.get(TOPIC_ON_TOPIC_EXIST))
                .map(Object::toString)
                .ifPresent(builder::setOnTopicExist);

        Optional.ofNullable(config.get(TOPIC_REPLICATION_FACTOR))
                .map(Object::toString)
                .ifPresent(builder::setReplicationFactor);

        Optional.ofNullable(config.get(TOPIC_PARTITIONS))
                .map(this::parseInt)
                .ifPresent(builder::setPartitions);

        builder.setConfigs(extractConfigSubset(config, TOPIC_CONFIGS));

        return builder.build();
    }

    private Map<String, Object> extractConfigSubset(Map<String, Object> config, String prefix) {
        return config.entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .collect(Collectors.toMap(
                        e -> e.getKey().substring(prefix.length()),
                        e -> Optional.ofNullable(e.getValue())
                                .map(Object::toString)
                                .orElse(null)));
    }

    private int parseInt(Object o) {
        if (o == null) {
            return 0;
        } else if (o instanceof Integer) {
            return (Integer) o;
        } else if (o instanceof Double) {
            return ((Double) o).intValue();
        } else if (o instanceof String) {
            return Integer.parseInt(o.toString());
        } else {
            throw new RuntimeException(String.format("Object %s can't be casted to Integer", o));
        }
    }
}
