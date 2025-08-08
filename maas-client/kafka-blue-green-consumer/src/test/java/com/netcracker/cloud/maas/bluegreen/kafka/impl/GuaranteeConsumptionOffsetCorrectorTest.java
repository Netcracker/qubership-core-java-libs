package com.netcracker.cloud.maas.bluegreen.kafka.impl;

import org.qubership.cloud.maas.bluegreen.kafka.ConsumerConsistencyMode;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.qubership.cloud.maas.bluegreen.kafka.TestUtil.*;

public class GuaranteeConsumptionOffsetCorrectorTest extends EventualOffsetCorrectorTest {

    @Override
    public void align(BGKafkaConsumerConfig config, OffsetsIndexer indexer, GroupId current, Consumer consumer) {
        Mockito.when(config.getConsistencyMode()).thenReturn(ConsumerConsistencyMode.GUARANTEE_CONSUMPTION);
        new OffsetsManager(config, consumer).alignOffset(current);
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_ActiveIdle_Commit")
    public void testInheritanceFor_ActiveIdle_Commit(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(
                topicOffset(0, 23),
                topicOffset(1, 20)
        ));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_Active_Promote")
    public void testInheritanceFor_Active_Promote(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(
                topicOffset(0, 45),
                topicOffset(1, 40)
        ));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_Active_Promote_ExistingGroupsFromPrevRollback")
    public void testInheritanceFor_Active_Promote_ExistingGroupsFromPrevRollback(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(
                topicOffset(0, 67),
                topicOffset(1, 60)
        ));
    }

    @Override
    @ParameterizedTest
    @MethodSource("source_testInheritanceFor_Active_Rollback")
    public void testInheritanceFor_Active_Rollback(GroupId current, BGKafkaConsumerConfig config, AdminAdapter adminAdapter, OffsetsIndexer indexer, Consumer consumer) {
        align(config, indexer, current, consumer);
        Mockito.verify(consumer).commitSync(topicOffsetMap(
                topicOffset(0, 56),
                topicOffset(1, 50)
        ));
    }
}
