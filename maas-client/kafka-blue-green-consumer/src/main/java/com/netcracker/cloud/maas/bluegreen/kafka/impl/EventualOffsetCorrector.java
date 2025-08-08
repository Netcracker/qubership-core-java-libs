package com.netcracker.cloud.maas.bluegreen.kafka.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.qubership.cloud.bluegreen.api.model.State.*;

@Slf4j
public class EventualOffsetCorrector extends OffsetCorrector {
    public EventualOffsetCorrector(Consumer<?, ?> consumer, BGKafkaConsumerConfig config, OffsetsIndexer indexer) {
        super(consumer, config, indexer);
    }

    @Override
    protected Map<TopicPartition, OffsetAndMetadata> inherit(GroupId current, Set<GroupIdWithOffset> previous) {
        log.info("Resolve offsets for versioned group: {} from previous stage offsets: {}", current, previous);
        Map<TopicPartition, OffsetAndMetadata> result;
        if (current instanceof PlainGroupId) {
            if (previous.isEmpty()) {
                /* non BG case */
                result = Map.of();
            } else {
                /* transition from old BG implementation */
                // inherit from old ACTIVE group
                result = previous.stream().filter(gwo -> gwo.groupId() instanceof BG1VersionedGroupId ovg &&
                                Objects.equals(ovg.getStage(), BG1VersionedGroupId.ACTIVE_STAGE))
                        .findFirst().map(GroupIdWithOffset::offset).orElse(Map.of());
                if (result.isEmpty()) {
                    /* destroy BG domain case */
                    // inherit from ACTIVE group
                    result = previous.stream().filter(gwo -> gwo.groupId() instanceof VersionedGroupId vg && vg.getState() == ACTIVE)
                            .findFirst().map(GroupIdWithOffset::offset).orElse(Map.of());
                }
            }
        } else if (current instanceof VersionedGroupId currentVersioned) {
            StatesComparePredicate predicate = new StatesComparePredicate(currentVersioned);
            if (predicate.test(ACTIVE, IDLE)) {
                Optional<Map<TopicPartition, OffsetAndMetadata>> plainOffset = previous.stream()
                        .filter(gwo -> gwo.groupId() instanceof PlainGroupId)
                        .map(GroupIdWithOffset::offset)
                        .findFirst();
                if (plainOffset.isPresent()) {
                    /* InitDomain case */
                    result = plainOffset.get();
                } else {
                    /* Commit case */
                    // inherit from ACTIVE group
                    result = previous.stream().filter(gwo -> gwo.groupId() instanceof VersionedGroupId vg && vg.getState() == ACTIVE)
                            .findFirst().map(GroupIdWithOffset::offset).orElse(Map.of());
                }
            } else if (predicate.test(ACTIVE, CANDIDATE)) {
                Optional<Map<TopicPartition, OffsetAndMetadata>> activeIdleOffset = previous.stream()
                        .filter(gwo -> new StatesComparePredicate(gwo.groupId()).test(ACTIVE, IDLE))
                        .findFirst().map(GroupIdWithOffset::offset);
                if (activeIdleOffset.isPresent()) {
                    /* Warmup case */
                    result = activeIdleOffset.get();
                } else {
                    /* Rollback case */
                    result = previous.stream()
                            .filter(gwo -> new StatesComparePredicate(gwo.groupId()).test(ACTIVE, LEGACY))
                            .findFirst().map(GroupIdWithOffset::offset)
                            .orElse(Map.of());
                }
            } else if (predicate.test(ACTIVE, LEGACY) || predicate.test(LEGACY, ACTIVE)) {
                /* Promote case */
                // inherit from ACTIVE group
                result = previous.stream().filter(gwo -> gwo.groupId() instanceof VersionedGroupId vg && vg.getState() == ACTIVE)
                        .findFirst().map(GroupIdWithOffset::offset).orElse(Map.of());
            } else if (predicate.test(CANDIDATE, ACTIVE)) {
                /* Warmup case */
                /* Rollback case */
                // inherit from ACTIVE group
                result = previous.stream().filter(gwo -> gwo.groupId() instanceof VersionedGroupId vg && vg.getState() == ACTIVE)
                        .findFirst().map(GroupIdWithOffset::offset).orElse(Map.of());
            } else {
                log.warn("Unregistered states pair: '{}'+'{}' of consumer group: {}",
                        currentVersioned.getState(), currentVersioned.getSiblingState(), currentVersioned);
                result = Map.of();
            }
        } else {
            log.warn("Unknown groupId type: {}", current);
            result = Map.of();
        }
        log.info("Resolved offsets: {}", result);
        return result;
    }
}
