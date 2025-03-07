package org.qubership.cloud.maas.bluegreen.kafka.impl;

import org.qubership.cloud.maas.bluegreen.kafka.OffsetSetupStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public abstract class OffsetCorrector {
    protected BGKafkaConsumerConfig config;
    protected final OffsetsIndexer indexer;
    protected final Consumer<?, ?> consumer;

    public OffsetCorrector(Consumer<?, ?> consumer, BGKafkaConsumerConfig config, OffsetsIndexer indexer) {
        this.consumer = consumer;
        this.config = config;
        this.indexer = indexer;
    }

    public Map<TopicPartition, OffsetAndMetadata> align(GroupId current) {
        // todo remove oldFormatBgVersionsExist() check when oldFormats versioned groups are deleted for good
        if (indexer.exists(current) && (!indexer.bg1VersionsExist() || indexer.bg1VersionsMigrated())) {
            Map<TopicPartition, OffsetAndMetadata> committed = consumer.committed(getTopicsPartitions());
            if(committed.values().stream().anyMatch(Objects::nonNull)) {
                log.info("Skip group id offsets corrections for: {}", current);
                return committed.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> Optional.ofNullable(entry.getValue()).orElse(new OffsetAndMetadata(0))));
            }
        }
        Set<GroupIdWithOffset> prevIds = indexer.findPreviousStateOffsets(current);
        log.info("Inherit offset from previous: {}", prevIds);
        Map<TopicPartition, OffsetAndMetadata> proposedOffset = inherit(current, prevIds);
        if (proposedOffset.isEmpty()) {
            OffsetSetupStrategy offsetSetupStrategy;
            if (current instanceof PlainGroupId) {
                offsetSetupStrategy = config.getActiveOffsetSetupStrategy();
            } else if (current instanceof VersionedGroupId vg) {
                offsetSetupStrategy = switch (vg.getState()) {
                    case ACTIVE -> config.getActiveOffsetSetupStrategy();
                    case CANDIDATE -> config.getCandidateOffsetSetupStrategy();
                    default -> {
                        OffsetSetupStrategy strategy = OffsetSetupStrategy.rewind(Duration.ofMinutes(5));
                        log.warn("No proposed offset resolved for state '{}'. Using default: '{}'", vg.getState(), strategy);
                        yield strategy;
                    }
                };
            } else {
                throw new IllegalArgumentException("Invalid current groupId type");
            }
            proposedOffset = install(offsetSetupStrategy);
        }
        log.info("Alter group '{}' offset to {}", current, proposedOffset);
        consumer.commitSync(proposedOffset);
        if (indexer.bg1VersionsExist() && !indexer.bg1VersionsMigrated()) {
            indexer.createMigrationDoneFromBg1MarkerGroup();
        }
        return proposedOffset;
    }

    protected Map<TopicPartition, OffsetAndMetadata> install(OffsetSetupStrategy strategy) {
        // need to get all topics-partitions used by this consumer group
        log.debug("Installing offsets for strategy: {}", strategy);
        Set<TopicPartition> topicPartitions = getTopicsPartitions();
        Map<TopicPartition, Long> offsets;
        if (strategy == OffsetSetupStrategy.EARLIEST) {
            offsets = consumer.beginningOffsets(topicPartitions);
        } else if (strategy == OffsetSetupStrategy.LATEST) {
            offsets = consumer.endOffsets(topicPartitions);
        } else {
            long shiftedToTime = Instant.now().minus(strategy.getShift()).toEpochMilli();
            Map<TopicPartition, Long> query = topicPartitions.stream().collect(Collectors.toMap(tp -> tp, tp -> shiftedToTime));
            Map<TopicPartition, OffsetAndTimestamp> found = consumer.offsetsForTimes(query);
            Map<TopicPartition, Long> fallback = consumer.endOffsets(topicPartitions);

            offsets = topicPartitions.stream().collect(Collectors.toMap(topicPartition -> topicPartition,
                    topicPartition -> Optional.ofNullable(found.get(topicPartition)).map(OffsetAndTimestamp::offset).orElseGet(() -> {
                        log.warn("No time based offset found for topic-partition: '{}'. Fallback strategy: '{}'", topicPartition, strategy);
                        Long offset = fallback.get(topicPartition);
                        if (offset == null) {
                            throw new IllegalStateException(String.format("No offset found for topicPartition=%s", topicPartition));
                        }
                        return offset;
                    })));
        }
        log.debug("Offsets to install: {}", offsets);
        return offsets.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new OffsetAndMetadata(e.getValue())));
    }

    private Set<TopicPartition> getTopicsPartitions() {
        return config.getTopics().stream()
                .map(consumer::partitionsFor)
                .flatMap(infos -> infos.stream().map((PartitionInfo p) -> new TopicPartition(p.topic(), p.partition())))
                .collect(Collectors.toSet());
    }

    protected abstract Map<TopicPartition, OffsetAndMetadata> inherit(GroupId current, Set<GroupIdWithOffset> previous);
}
