package com.netcracker.cloud.maas.bluegreen.kafka;

import org.qubership.cloud.bluegreen.api.model.BlueGreenState;
import org.qubership.cloud.bluegreen.api.model.NamespaceVersion;
import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.model.Version;
import org.qubership.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import org.qubership.cloud.framework.contexts.xversion.XVersionContextObject;
import org.qubership.cloud.maas.bluegreen.kafka.impl.AdminAdapter;
import org.qubership.cloud.maas.bluegreen.kafka.impl.BGKafkaConsumerConfig;
import org.qubership.cloud.maas.bluegreen.kafka.impl.BGKafkaConsumerImpl;
import org.qubership.cloud.maas.bluegreen.kafka.impl.VersionedGroupId;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.qubership.cloud.maas.bluegreen.kafka.TestUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class BGKafkaConsumerUnitTest {
    static String NAMESPACE_1 = "test-ns-1";
    static String NAMESPACE_2 = "test-ns-2";
    static Supplier<String> M2M_TOKEN_SUPPLIER = () -> "fake";
    private Duration POLL_TIMEOUT = Duration.ofSeconds(10);

    @Test
    void testConsecutiveStateChanges() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        var connectionProperties = Map.<String, Object>of("group.id", TEST_GROUP_NAME);

        var v1 = new Version("v1");
        var v2 = new Version("v2");

        InMemoryBlueGreenStatePublisher statePublisher = new InMemoryBlueGreenStatePublisher(NAMESPACE_1);
        Consumer consumer = Mockito.mock(Consumer.class);
        AdminAdapter adminAdapter = Mockito.mock(AdminAdapter.class);
        AtomicInteger listGroupsCounter = new AtomicInteger(0);
        String groupAForState1 = String.format("%s-v1-a-2018-12-03_19-34-50", TEST_GROUP_NAME);
        String groupCForState1 = String.format("%s-v1-c-2018-12-03_19-34-50", TEST_GROUP_NAME);
        Mockito.doAnswer(i -> {
            int invocation = listGroupsCounter.incrementAndGet();
            if (invocation == 1) {
                return List.of();
            } else if (invocation == 2) {
                return List.of(
                        new ConsumerGroupListing(groupAForState1, true),
                        new ConsumerGroupListing(groupCForState1, true)
                );
            } else {
                throw new IllegalStateException("Unexpected listConsumerGroup() invocation");
            }
        }).when(adminAdapter).listConsumerGroup();

        Mockito.when(consumer.partitionsFor(TEST_TOPIC_NAME)).thenReturn(List.of(partitionInfo(0)));
        Mockito.when(consumer.offsetsForTimes(Mockito.any())).thenReturn(Map.of());
        Mockito.when(consumer.endOffsets(Mockito.any())).thenReturn(topicOffsetLongMap(topicOffsetLong(0, 1)));

        Mockito.when(adminAdapter.listConsumerGroupOffsets(groupAForState1))
                .thenReturn(topicOffsetMap(topicOffset(0, 1)));
        Mockito.when(adminAdapter.listConsumerGroupOffsets(groupCForState1))
                .thenReturn(topicOffsetMap(topicOffset(0, 1)));

        try (BGKafkaConsumer<String, String> consumerActive = new BGKafkaConsumerImpl<>(BGKafkaConsumerConfig.builder(connectionProperties,
                        TEST_TOPIC_NAME, M2M_TOKEN_SUPPLIER, statePublisher)
                .deserializers(new StringDeserializer(), new StringDeserializer())
                .consistencyMode(ConsumerConsistencyMode.GUARANTEE_CONSUMPTION)
                .consumerSupplier(props -> consumer)
                .adminAdapterSupplier(props -> adminAdapter).build())) {

            Field bgStateRefField = consumerActive.getClass().getDeclaredField("bgStateRef");
            bgStateRefField.setAccessible(true);
            AtomicReference<BlueGreenState> bgStateRef = (AtomicReference) bgStateRefField.get(consumerActive);
            AtomicReference<BlueGreenState> bgStateRefSpied = Mockito.spy(bgStateRef);
            bgStateRefField.set(consumerActive, bgStateRefSpied);

            BlueGreenState bgState1 = new BlueGreenState(
                    new NamespaceVersion(NAMESPACE_1, State.ACTIVE, v1),
                    new NamespaceVersion(NAMESPACE_2, State.CANDIDATE, v2),
                    VersionedGroupId.toOffsetDateTime("2018-12-03_19-34-50"));

            BlueGreenState bgState2 = new BlueGreenState(
                    new NamespaceVersion(NAMESPACE_1, State.LEGACY, v1),
                    new NamespaceVersion(NAMESPACE_2, State.ACTIVE, v2),
                    VersionedGroupId.toOffsetDateTime("2018-12-05_19-34-50"));

            CountDownLatch waitUntilFirstBgStateRead = new CountDownLatch(1);
            CountDownLatch waitUntilSecondStateSet = new CountDownLatch(1);

            Mockito.doAnswer(i -> {
                waitUntilFirstBgStateRead.countDown();
                Object result = i.callRealMethod();
                // wait until second state was set
                waitUntilSecondStateSet.await();
                return result;
            }).when(bgStateRefSpied).getAndSet(Mockito.any());

            ConsumerRecord<String, String> record1_1 = new ConsumerRecord<>(TEST_TOPIC_NAME, 0, 1, "key-1.1", "value-1.1");
            ConsumerRecord<String, String> record1_2 = new ConsumerRecord<>(TEST_TOPIC_NAME, 0, 2, "key-1.2", "value-1.2");
            record1_2.headers().add(XVersionContextObject.X_VERSION_SERIALIZATION_NAME, "v1".getBytes());
            ConsumerRecord<String, String> record1_3 = new ConsumerRecord<>(TEST_TOPIC_NAME, 0, 3, "key-1.3", "value-1.3");
            record1_3.headers().add(XVersionContextObject.X_VERSION_SERIALIZATION_NAME, "v2".getBytes());

            ConsumerRecord<String, String> record2_1 = new ConsumerRecord<>(TEST_TOPIC_NAME, 0, 4, "key-2.1", "value-2.1");
            ConsumerRecord<String, String> record2_2 = new ConsumerRecord<>(TEST_TOPIC_NAME, 0, 5, "key-2.2", "value-2.2");
            record2_2.headers().add(XVersionContextObject.X_VERSION_SERIALIZATION_NAME, "v1".getBytes());
            ConsumerRecord<String, String> record2_3 = new ConsumerRecord<>(TEST_TOPIC_NAME, 0, 6, "key-2.3", "value-2.3");
            record2_3.headers().add(XVersionContextObject.X_VERSION_SERIALIZATION_NAME, "v2".getBytes());

            ConsumerRecords<String, String> records1 = new ConsumerRecords<>(Map.of(
                    new TopicPartition(TEST_TOPIC_NAME, 0),
                    List.of(record1_1, record1_2, record1_3)));
            ConsumerRecords<String, String> records2 = new ConsumerRecords<>(Map.of(
                    new TopicPartition(TEST_TOPIC_NAME, 0),
                    List.of(record2_1, record2_2, record2_3)));

            AtomicInteger pollCounter = new AtomicInteger();
            Mockito.when(consumer.poll(Mockito.any())).thenAnswer(i -> {
                int pollNumber = pollCounter.incrementAndGet();
                if (pollNumber == 1) {
                    return records1;
                } else if (pollNumber == 2) {
                    return records2;
                }
                return null;
            });
            statePublisher.setBlueGreenState(bgState1);
            executorService.submit(() -> {
                try {
                    waitUntilFirstBgStateRead.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                statePublisher.setBlueGreenState(bgState2);
                waitUntilSecondStateSet.countDown();
            });

            RecordsBatch<String, String> bgRecords1 = consumerActive.poll(POLL_TIMEOUT).get();
            assertEquals(2, bgRecords1.getBatch().size());
            assertEquals("key-1.1", bgRecords1.getBatch().get(0).getConsumerRecord().key());
            assertEquals("key-1.2", bgRecords1.getBatch().get(1).getConsumerRecord().key());

            RecordsBatch<String, String> bgRecords2 = consumerActive.poll(POLL_TIMEOUT).get();
            assertEquals(1, bgRecords2.getBatch().size());
            assertEquals("key-2.2", bgRecords2.getBatch().get(0).getConsumerRecord().key());
        }
    }

    @Test
    void testIdleState() throws Exception {

        var connectionProperties = Map.<String, Object>of("group.id", TEST_GROUP_NAME);

        var v1 = new Version("v1");
        var v2 = new Version("v2");

        InMemoryBlueGreenStatePublisher statePublisher = new InMemoryBlueGreenStatePublisher(NAMESPACE_1);
        Consumer consumer = Mockito.mock(Consumer.class);
        AdminAdapter adminAdapter = Mockito.mock(AdminAdapter.class);
        Mockito.when(adminAdapter.listConsumerGroup()).thenReturn(List.of());
        Mockito.when(consumer.partitionsFor(TEST_TOPIC_NAME)).thenReturn(List.of(partitionInfo(0)));
        Mockito.when(consumer.offsetsForTimes(Mockito.any())).thenReturn(Map.of());
        Mockito.when(consumer.endOffsets(Mockito.any())).thenReturn(topicOffsetLongMap(topicOffsetLong(0, 1)));

        try (BGKafkaConsumer<String, String> consumerCandidate = new BGKafkaConsumerImpl<>(BGKafkaConsumerConfig.builder(connectionProperties,
                        TEST_TOPIC_NAME, M2M_TOKEN_SUPPLIER, statePublisher)
                .deserializers(new StringDeserializer(), new StringDeserializer())
                .consistencyMode(ConsumerConsistencyMode.GUARANTEE_CONSUMPTION)
                .consumerSupplier(props -> consumer)
                .adminAdapterSupplier(props -> adminAdapter).build())) {

            BlueGreenState bgState1 = new BlueGreenState(
                    new NamespaceVersion(NAMESPACE_1, State.CANDIDATE, v1),
                    new NamespaceVersion(NAMESPACE_2, State.ACTIVE, v2),
                    VersionedGroupId.toOffsetDateTime("2018-12-03_19-34-50"));

            BlueGreenState bgState2 = new BlueGreenState(
                    new NamespaceVersion(NAMESPACE_1, State.IDLE, v1),
                    new NamespaceVersion(NAMESPACE_2, State.ACTIVE, v2),
                    VersionedGroupId.toOffsetDateTime("2018-12-05_19-34-50"));

            ConsumerRecord<String, String> record1 = new ConsumerRecord<>(TEST_TOPIC_NAME, 0, 1, "key-1", "value-1");
            record1.headers().add(XVersionContextObject.X_VERSION_SERIALIZATION_NAME, "v1".getBytes());
            ConsumerRecords<String, String> records1 = new ConsumerRecords<>(Map.of(
                    new TopicPartition(TEST_TOPIC_NAME, 0),
                    List.of(record1)));
            Mockito.when(consumer.poll(Mockito.any())).thenReturn(records1);

            statePublisher.setBlueGreenState(bgState1);

            Optional<RecordsBatch<String, String>> bgRecords1 = consumerCandidate.poll(POLL_TIMEOUT);
            assertTrue(bgRecords1.isPresent());
            assertEquals(1, bgRecords1.get().getBatch().size());
            assertEquals("key-1", bgRecords1.get().getBatch().get(0).getConsumerRecord().key());

            statePublisher.setBlueGreenState(bgState2);

            Optional<RecordsBatch<String, String>> bgRecords2 = consumerCandidate.poll(Duration.ofSeconds(1));
            assertTrue(bgRecords2.isEmpty());
        }
    }

    @Test
    void testXVersionHeader() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TEST_TOPIC_NAME, 1, 1, "key-1", "value-1");
        record.headers().add(XVersionContextObject.X_VERSION_SERIALIZATION_NAME, "v1".getBytes());
        String xVersion = BGKafkaConsumerImpl.extractVersionHeader(record);
        Assertions.assertEquals("v1", xVersion);
    }

    @Test
    void testXVersionHeader_Missed() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TEST_TOPIC_NAME, 1, 1, "key-1", "value-1");
        String xVersion = BGKafkaConsumerImpl.extractVersionHeader(record);
        Assertions.assertEquals("", xVersion);
    }

    @Test
    void testXVersionHeaderLowerCase() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TEST_TOPIC_NAME, 1, 1, "key-1", "value-1");
        record.headers().add(XVersionContextObject.X_VERSION_SERIALIZATION_NAME.toLowerCase(), "v1".getBytes());
        String xVersion = BGKafkaConsumerImpl.extractVersionHeader(record);
        Assertions.assertEquals("v1", xVersion);
    }
}
