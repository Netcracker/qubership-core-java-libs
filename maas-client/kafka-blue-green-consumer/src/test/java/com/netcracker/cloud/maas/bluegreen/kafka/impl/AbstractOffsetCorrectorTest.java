package com.netcracker.cloud.maas.bluegreen.kafka.impl;

import com.netcracker.cloud.bluegreen.api.model.State;
import com.netcracker.cloud.bluegreen.api.model.Version;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.netcracker.cloud.maas.bluegreen.kafka.TestUtil.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;

public abstract class AbstractOffsetCorrectorTest {

    public abstract void align(BGKafkaConsumerConfig config, OffsetsIndexer indexer, GroupId current, Consumer consumer);

    public abstract void testInheritanceFor_ActiveIdle_InitDomain(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_ActiveIdle_InitDomain_NoPlainOffset(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_ActiveIdle_Commit(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_Active_Warmup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_Active_Promote(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_Active_Promote_NoPrevActiveGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_Active_Promote_NoPrevCandidateGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_Active_Promote_ExistingGroupsFromPrevRollback(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_Active_Rollback(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_Active_Rollback_NoPrevActiveGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_Active_Rollback_NoPrevLegacyGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_Candidate_Warmup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_Candidate_Warmup_NoPrevActiveIdleGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_Candidate_Rollback(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_Candidate_Rollback_NoPrevActiveGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_Legacy_Promote(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_Legacy_Promote_NoPrevActiveGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_NoBg_RollingUpdate_PrevOldBgActiveGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testInheritanceFor_NoBg_RollingUpdate_PrevOldAndMigratedBgActiveGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    public abstract void testGroupsNotDeleted(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer);

    // Versions nd scenarios are taken from bg2-states-kafka-offsets.md

    public static Stream<Arguments> source_testInheritanceFor_ActiveIdle_InitDomain() {
        return arguments("test-v1-a_i-2023-07-07_10-30-00",
                adminAdapter(group(TEST_GROUP_NAME, topicOffset(0, 1))));
    }

    public static Stream<Arguments> source_testInheritanceFor_ActiveIdle_InitDomain_NoPlainOffset() {
        AdminAdapter adminAdapter = adminAdapter();
        Consumer consumer = Mockito.mock(Consumer.class);
        Mockito.when(adminAdapter.listConsumerGroupOffsets(Mockito.eq("test-v1-a_i-2023-07-07_10-30-00")))
                .thenReturn(topicOffsetMap(topicOffset(0, 1)));

        Mockito.when(consumer.partitionsFor(TEST_TOPIC_NAME)).thenReturn(List.of(new PartitionInfo(TEST_TOPIC_NAME, 0, null, null, null)));
        Mockito.when(consumer.endOffsets(anyCollection())).thenReturn(topicOffsetLongMap(topicOffsetLong(0, 1)));
        Mockito.when(consumer.offsetsForTimes(anyMap())).thenReturn(topicOffsetTimestampMap(topicOffsetTimestamp(0, 1, 0)));
        return arguments("test-v1-a_i-2023-07-07_10-30-00", adminAdapter, consumer);
    }

    public static Stream<Arguments> source_testInheritanceFor_ActiveIdle_Commit() {
        return arguments("test-v1-a_i-2023-07-07_12-30-00",
                adminAdapter(
                        group("test-v1-a_c-2023-07-07_11-30-00",
                                topicOffset(0, 23),
                                topicOffset(1, 24)
                        ),
                        group("test-v2-c_a-2023-07-07_11-30-00",
                                topicOffset(0, 24),
                                topicOffset(1, 20)
                        )
                ));
    }

    public static Stream<Arguments> source_testInheritanceFor_Active_Warmup() {
        return arguments("test-v1-a_c-2023-07-07_11-30-00",
                adminAdapter(
                        group(TEST_GROUP_NAME, topicOffset(0, 1)),
                        group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 12))
                ));
    }

    public static Stream<Arguments> source_testInheritanceFor_Active_Promote() {
        return arguments("test-v3-a_l-2023-07-07_14-30-00",
                adminAdapter(
                        group(TEST_GROUP_NAME, topicOffset(0, 1)),
                        group("test-v1-a_c-2023-07-07_13-30-00",
                                topicOffset(0, 45),
                                topicOffset(1, 46)
                        ),
                        group("test-v3-c_a-2023-07-07_13-30-00",
                                topicOffset(0, 46),
                                topicOffset(1, 40)
                        )
                ));
    }

    public static Stream<Arguments> source_testInheritanceFor_Active_Promote_NoPrevActiveGroup() {
        return arguments("test-v3-a_l-2023-07-07_14-30-00",
                adminAdapter(
                        group(TEST_GROUP_NAME, topicOffset(0, 1)),
                        group("test-v3-c_a-2023-07-07_13-30-00",
                                topicOffset(0, 46),
                                topicOffset(1, 40)
                        )
                ));
    }

    public static Stream<Arguments> source_testInheritanceFor_Active_Promote_NoPrevCandidateGroup() {
        return arguments("test-v3-a_l-2023-07-07_14-30-00",
                adminAdapter(
                        group(TEST_GROUP_NAME, topicOffset(0, 1)),
                        group("test-v1-a_c-2023-07-07_13-30-00",
                                topicOffset(0, 45),
                                topicOffset(1, 46)
                        )
                ));
    }

    public static Stream<Arguments> source_testInheritanceFor_Active_Promote_ExistingGroupsFromPrevRollback() {
        return arguments("test-v3-a_l-2023-07-07_16-30-00",
                adminAdapter(
                        // after Rollback
                        group("test-v1-a_c-2023-07-07_15-30-00",
                                topicOffset(0, 67),
                                topicOffset(1, 68)
                        ),
                        group("test-v3-c_a-2023-07-07_15-30-00",
                                topicOffset(0, 68),
                                topicOffset(1, 60)
                        ),
                        // before Rollback
                        group("test-v3-a_l-2023-07-07_14-30-00",
                                topicOffset(0, 57),
                                topicOffset(1, 58)
                        ),
                        group("test-v1-l_a-2023-07-07_14-30-00",
                                topicOffset(0, 56),
                                topicOffset(1, 50)
                        )
                ));
    }

    public static Stream<Arguments> source_testInheritanceFor_Active_Rollback() {
        return arguments("test-v1-a_c-2023-07-07_15-30-00",
                adminAdapter(
                        group(TEST_GROUP_NAME, topicOffset(0, 1)),
                        group("test-v3-a_l-2023-07-07_14-30-00",
                                topicOffset(0, 57),
                                topicOffset(1, 58)
                        ),
                        group("test-v1-l_a-2023-07-07_14-30-00",
                                topicOffset(0, 56),
                                topicOffset(1, 50)
                        )
                ));
    }

    public static Stream<Arguments> source_testInheritanceFor_Active_Rollback_NoPrevActiveGroup() {
        return arguments("test-v3-a_c-2023-07-07_14-30-00",
                adminAdapter(
                        group(TEST_GROUP_NAME, topicOffset(0, 1)),
                        group("test-v1-l_a-2023-07-07_14-30-00",
                                topicOffset(0, 56),
                                topicOffset(1, 50)
                        )
                ));
    }

    public static Stream<Arguments> source_testInheritanceFor_Active_Rollback_NoPrevLegacyGroup() {
        return arguments("test-v1-a_c-2023-07-07_15-30-00",
                adminAdapter(
                        group(TEST_GROUP_NAME, topicOffset(0, 1)),
                        group("test-v3-a_l-2023-07-07_14-30-00",
                                topicOffset(0, 57),
                                topicOffset(1, 58)
                        )
                ));
    }

    public static Stream<Arguments> source_testInheritanceFor_Candidate_Warmup() {
        return arguments("test-v2-c_a-2023-07-07_11-30-00",
                adminAdapter(
                        group(TEST_GROUP_NAME, topicOffset(0, 1)),
                        group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 12))
                ));
    }

    public static Stream<Arguments> source_testInheritanceFor_Candidate_Warmup_NoPrevActiveIdleGroup() {
        return arguments("test-v2-c_a-2023-07-07_11-30-00",
                adminAdapter(
                        group(TEST_GROUP_NAME, topicOffset(0, 1))
                ));
    }

    public static Stream<Arguments> source_testInheritanceFor_Candidate_Rollback() {
        return arguments("test-v3-c_a-2023-07-07_15-30-00",
                adminAdapter(
                        group(TEST_GROUP_NAME, topicOffset(0, 1)),
                        group("test-v3-a_l-2023-07-07_14-30-00",
                                topicOffset(0, 57),
                                topicOffset(1, 58)
                        ),
                        group("test-v1-l_a-2023-07-07_14-30-00",
                                topicOffset(0, 56),
                                topicOffset(1, 50)
                        )
                ));
    }

    public static Stream<Arguments> source_testInheritanceFor_Candidate_Rollback_NoPrevActiveGroup() {
        return arguments("test-v3-c_a-2023-07-07_15-30-00",
                adminAdapter(
                        group(TEST_GROUP_NAME, topicOffset(0, 1)),
                        group("test-v1-l_a-2023-07-07_14-30-00",
                                topicOffset(0, 56),
                                topicOffset(1, 50)
                        )
                ));
    }

    public static Stream<Arguments> source_testInheritanceFor_Legacy_Promote() {
        return arguments("test-v1-l_a-2023-07-07_14-30-00",
                adminAdapter(
                        group(TEST_GROUP_NAME, topicOffset(0, 1)),
                        group("test-v1-a_c-2023-07-07_13-30-00",
                                topicOffset(0, 45),
                                topicOffset(1, 46)
                        ),
                        group("test-v3-c_a-2023-07-07_13-30-00",
                                topicOffset(0, 46),
                                topicOffset(1, 40)
                        )
                ));
    }

    public static Stream<Arguments> source_testInheritanceFor_Legacy_Promote_NoPrevActiveGroup() {
        return arguments("test-v1-l_a-2023-07-07_14-30-00",
                adminAdapter(
                        group(TEST_GROUP_NAME, topicOffset(0, 1)),
                        group("test-v3-c_a-2023-07-07_13-30-00",
                                topicOffset(0, 46),
                                topicOffset(1, 40)
                        )
                ));
    }

    public static Stream<Arguments> source_testInheritanceFor_NoBg_RollingUpdate_PrevOldBgActiveGroup() {
        return arguments(TEST_GROUP_NAME,
                adminAdapter(
                        group(TEST_GROUP_NAME, topicOffset(0, 1), topicOffset(1, 2)),
                        group("test-v1v1a1701191191",
                                topicOffset(0, 45),
                                topicOffset(1, 46)
                        ),
                        group("test-v1v1a1701191991",
                                topicOffset(0, 100),
                                topicOffset(1, 101))
                ));
    }

    public static Stream<Arguments> source_testInheritanceFor_NoBg_RollingUpdate_PrevOldAndMigratedBgActiveGroup() {
        var groups = new Map.Entry[] {
            group(TEST_GROUP_NAME, topicOffset(0, 200), topicOffset(1, 201)),
                    group("test-v1v1a1701191191",
                            topicOffset(0, 45),
                            topicOffset(1, 46)
                    ),
                    group("test-v1v1a1701191991",
                            topicOffset(0, 100),
                            topicOffset(1, 101)),
                    group("test-v1v1M1701191991",
                            topicOffset(0, 100),
                            topicOffset(1, 101))
        };
        AdminAdapter adminAdapter = adminAdapter(groups);
        Consumer consumer = consumer(groups);
        return arguments(TEST_GROUP_NAME, adminAdapter, consumer);
    }

    public static Stream<Arguments> source_testGroupsNotDeleted() {
        OffsetDateTime startTime = OffsetDateTime.ofInstant(Instant.ofEpochSecond(1701191191), ZoneOffset.UTC);
        Map.Entry<ConsumerGroupListing, Map<TopicPartition, OffsetAndMetadata>>[] groups = IntStream.range(0, 50).boxed().map(i -> {
            VersionedGroupId groupId = new VersionedGroupId(TEST_GROUP_NAME, new Version(1), State.ACTIVE, State.CANDIDATE, startTime.plusMinutes(i));
            return group(groupId.toString(), topicOffset(0, i));
        }).toArray(Map.Entry[]::new);
        return arguments(TEST_GROUP_NAME, adminAdapter(groups));
    }


}
