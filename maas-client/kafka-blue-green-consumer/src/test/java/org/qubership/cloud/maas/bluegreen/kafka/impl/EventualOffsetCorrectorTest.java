package org.qubership.cloud.maas.bluegreen.kafka.impl;

import org.qubership.cloud.maas.bluegreen.kafka.ConsumerConsistencyMode;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.qubership.cloud.maas.bluegreen.kafka.TestUtil.*;

public class EventualOffsetCorrectorTest extends AbstractOffsetCorrectorTest {
    @Override
    public void align(BGKafkaConsumerConfig config, OffsetsIndexer indexer, GroupId current, Consumer consumer) {
        Mockito.when(config.getConsistencyMode()).thenReturn(ConsumerConsistencyMode.EVENTUAL);
        new OffsetsManager(config, consumer).alignOffset(current);
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_ActiveIdle_InitDomain")
    public void testInheritanceFor_ActiveIdle_InitDomain(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(topicOffset(0, 1)));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_ActiveIdle_InitDomain_NoPlainOffset")
    public void testInheritanceFor_ActiveIdle_InitDomain_NoPlainOffset(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(topicOffset(0, 1)));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_ActiveIdle_Commit")
    public void testInheritanceFor_ActiveIdle_Commit(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer)
                .commitSync(topicOffsetMap(
                        topicOffset(0, 23),
                        topicOffset(1, 24)
                ));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_Active_Warmup")
    public void testInheritanceFor_Active_Warmup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(topicOffset(0, 12)));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_Active_Promote")
    public void testInheritanceFor_Active_Promote(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(
                topicOffset(0, 45),
                topicOffset(1, 46)
        ));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_Active_Promote_NoPrevActiveGroup")
    public void testInheritanceFor_Active_Promote_NoPrevActiveGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        Mockito.when(config.getConsumerSupplier()).thenReturn(p -> consumer);
        Mockito.when(adminAdapter.listConsumerGroupOffsets(Mockito.eq(current.toString())))
                .thenReturn(topicOffsetMap(topicOffset(0, 22), topicOffset(1, 21)));
        Mockito.when(consumer.offsetsForTimes(Mockito.any()))
                .thenReturn(topicOffsetTimestampMap(
                        topicOffsetTimestamp(0, 22, 0/* do not care about timestamp here */),
                        topicOffsetTimestamp(1, 21, 0/* do not care about timestamp here */)
                ));
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(
                topicOffset(0, 22),
                topicOffset(1, 21)
        ));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_Active_Promote_NoPrevCandidateGroup")
    public void testInheritanceFor_Active_Promote_NoPrevCandidateGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(
                topicOffset(0, 45),
                topicOffset(1, 46)
        ));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_Active_Promote_ExistingGroupsFromPrevRollback")
    public void testInheritanceFor_Active_Promote_ExistingGroupsFromPrevRollback(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(
                topicOffset(0, 67),
                topicOffset(1, 68)
        ));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_Active_Rollback")
    public void testInheritanceFor_Active_Rollback(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(
                topicOffset(0, 57),
                topicOffset(1, 58)
        ));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_Active_Rollback_NoPrevActiveGroup")
    public void testInheritanceFor_Active_Rollback_NoPrevActiveGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        Mockito.when(config.getConsumerSupplier()).thenReturn(p -> consumer);
        Mockito.when(adminAdapter.listConsumerGroupOffsets(Mockito.eq(current.toString())))
                .thenReturn(topicOffsetMap(topicOffset(0, 22), topicOffset(1, 21)));
        Mockito.when(consumer.offsetsForTimes(Mockito.any()))
                .thenReturn(topicOffsetTimestampMap(
                        topicOffsetTimestamp(0, 22, 0/* do not care about timestamp here */),
                        topicOffsetTimestamp(1, 21, 0/* do not care about timestamp here */)
                ));
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(
                topicOffset(0, 22),
                topicOffset(1, 21)
        ));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_Active_Rollback_NoPrevLegacyGroup")
    public void testInheritanceFor_Active_Rollback_NoPrevLegacyGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(
                topicOffset(0, 57),
                topicOffset(1, 58)
        ));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_Candidate_Warmup")
    public void testInheritanceFor_Candidate_Warmup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(topicOffset(0, 12)));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_Candidate_Warmup_NoPrevActiveIdleGroup")
    public void testInheritanceFor_Candidate_Warmup_NoPrevActiveIdleGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        Mockito.when(config.getConsumerSupplier()).thenReturn(p -> consumer);
        Mockito.when(adminAdapter.listConsumerGroupOffsets(Mockito.eq(current.toString())))
                .thenReturn(topicOffsetMap(topicOffset(0, 22), topicOffset(1, 21)));
        Mockito.when(consumer.offsetsForTimes(Mockito.any()))
                .thenReturn(topicOffsetTimestampMap(
                        topicOffsetTimestamp(0, 22, 0/* do not care about timestamp here */),
                        topicOffsetTimestamp(1, 21, 0/* do not care about timestamp here */)
                ));
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(
                topicOffset(0, 22),
                topicOffset(1, 21)
        ));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_Candidate_Rollback")
    public void testInheritanceFor_Candidate_Rollback(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(
                topicOffset(0, 57),
                topicOffset(1, 58)
        ));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_Candidate_Rollback_NoPrevActiveGroup")
    public void testInheritanceFor_Candidate_Rollback_NoPrevActiveGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        Mockito.when(config.getConsumerSupplier()).thenReturn(p -> consumer);
        Mockito.when(adminAdapter.listConsumerGroupOffsets(Mockito.eq(current.toString())))
                .thenReturn(topicOffsetMap(topicOffset(0, 22), topicOffset(1, 21)));
        Mockito.when(consumer.offsetsForTimes(Mockito.any()))
                .thenReturn(topicOffsetTimestampMap(
                        topicOffsetTimestamp(0, 22, 0/* do not care about timestamp here */),
                        topicOffsetTimestamp(1, 21, 0/* do not care about timestamp here */)
                ));
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(
                topicOffset(0, 22),
                topicOffset(1, 21)
        ));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_Legacy_Promote")
    public void testInheritanceFor_Legacy_Promote(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(
                topicOffset(0, 45),
                topicOffset(1, 46)
        ));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_Legacy_Promote_NoPrevActiveGroup")
    public void testInheritanceFor_Legacy_Promote_NoPrevActiveGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        Mockito.when(config.getConsumerSupplier()).thenReturn(p -> consumer);
        Mockito.when(adminAdapter.listConsumerGroupOffsets(Mockito.eq(current.toString())))
                .thenReturn(topicOffsetMap(topicOffset(0, 22), topicOffset(1, 21)));
        Mockito.when(consumer.offsetsForTimes(Mockito.any()))
                .thenReturn(topicOffsetTimestampMap(
                        topicOffsetTimestamp(0, 22, 0/* do not care about timestamp here */),
                        topicOffsetTimestamp(1, 21, 0/* do not care about timestamp here */)
                ));
        align(config, indexer, current, consumer);
        Mockito.verify(consumer)
                .commitSync(topicOffsetMap(
                        topicOffset(0, 22),
                        topicOffset(1, 21)
                ));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_NoBg_RollingUpdate_PrevOldBgActiveGroup")
    public void testInheritanceFor_NoBg_RollingUpdate_PrevOldBgActiveGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(
                topicOffset(0, 100),
                topicOffset(1, 101)
        ));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_NoBg_RollingUpdate_PrevOldAndMigratedBgActiveGroup")
    public void testInheritanceFor_NoBg_RollingUpdate_PrevOldAndMigratedBgActiveGroup(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer, Mockito.never()).commitSync(Mockito.anyMap());
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testGroupsNotDeleted")
    public void testGroupsNotDeleted(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(adminAdapter, Mockito.never()).deleteConsumerGroups(Mockito.anyCollection());
    }
}
