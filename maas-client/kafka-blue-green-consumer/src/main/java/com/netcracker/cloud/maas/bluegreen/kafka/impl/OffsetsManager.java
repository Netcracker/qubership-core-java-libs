package com.netcracker.cloud.maas.bluegreen.kafka.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;

@Slf4j
public class OffsetsManager {
    private final BGKafkaConsumerConfig config;
    private final Consumer<?, ?> consumer;

    public OffsetsManager(BGKafkaConsumerConfig config, Consumer<?, ?> consumer) {
        this.config = config;
        this.consumer = consumer;
    }

    public Map<TopicPartition, OffsetAndMetadata> alignOffset(GroupId groupId) {
        try (var admin = config.getAdminAdapterSupplier().apply(config.getProperties())) {
            OffsetsIndexer indexer = new OffsetsIndexer(groupId.getGroupIdPrefix(), admin);
            OffsetCorrector corrector = switch (config.getConsistencyMode()) {
                case EVENTUAL -> new EventualOffsetCorrector(consumer, config, indexer);
                case GUARANTEE_CONSUMPTION -> new GuaranteeConsistencyOffsetCorrector(consumer, config, indexer);
            };
            return corrector.align(groupId);
//            indexer.removeOldGroups(10); //todo design proper deletion (without deleting groups in use)
        }
    }
}
