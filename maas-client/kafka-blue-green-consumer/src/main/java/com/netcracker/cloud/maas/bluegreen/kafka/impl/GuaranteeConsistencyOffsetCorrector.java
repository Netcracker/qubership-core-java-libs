package com.netcracker.cloud.maas.bluegreen.kafka.impl;

import com.netcracker.cloud.bluegreen.api.model.State;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class GuaranteeConsistencyOffsetCorrector extends EventualOffsetCorrector {
    public GuaranteeConsistencyOffsetCorrector(Consumer<?, ?> consumer, BGKafkaConsumerConfig config, OffsetsIndexer indexer) {
        super(consumer, config, indexer);
    }

    @Override
    protected Map<TopicPartition, OffsetAndMetadata> inherit(GroupId current, Set<GroupIdWithOffset> previous) {
        log.info("Resolve offsets for versioned group: {} from previous stage offsets: {}", current, previous);
        Map<TopicPartition, OffsetAndMetadata> result;
        // Rollback case : (ACTIVE, CANDIDATE),
        // Promote case : (ACTIVE, LEGACY)
        // Commit case : (ACTIVE, IDLE)
        if (current instanceof VersionedGroupId currentVersioned &&
                (Optional.of(new StatesComparePredicate(currentVersioned))
                        .map(p -> p.test(State.ACTIVE, State.CANDIDATE) ||
                                p.test(State.ACTIVE, State.LEGACY) ||
                                p.test(State.ACTIVE, State.IDLE))
                        .orElse(false)) &&
                previous.stream().map(GroupIdWithOffset::groupId)
                        .filter(g -> g instanceof VersionedGroupId)
                        .map(VersionedGroupId.class::cast)
                        .map(VersionedGroupId::getState)
                        .anyMatch(s -> s == State.ACTIVE)) {
            result = previous.stream().map(GroupIdWithOffset::offset).reduce((o1, o2) -> {
                Set<TopicPartition> topicPartitions = Stream.concat(o1.keySet().stream(), o2.keySet().stream())
                        .collect(Collectors.toSet());
                return topicPartitions.stream().collect(Collectors.toMap(tp -> tp, tp -> {
                    OffsetAndMetadata offset1 = o1.get(tp);
                    OffsetAndMetadata offset2 = o2.get(tp);
                    if (offset1 == null && offset2 == null) {
                        // seems like impossible case
                        return new OffsetAndMetadata(0);
                    } else {
                        if (offset1 == null) {
                            return offset2;
                        } else if (offset2 == null) {
                            return offset1;
                        } else {
                            return offset1.offset() < offset2.offset() ? offset1 : offset2;
                        }
                    }
                }));
            }).orElseGet(Map::of);
        } else {
            result = super.inherit(current, previous);
        }
        log.info("Resolved offsets: {}", result);
        return result;
    }
}
