package com.netcracker.maas.declarative.kafka.client.impl.client.consumer.executor;

import com.netcracker.maas.declarative.kafka.client.impl.common.bg.KafkaConsumerConfiguration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.errors.FencedInstanceIdException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.netcracker.cloud.bluegreen.api.model.NamespaceVersion;
import com.netcracker.cloud.bluegreen.api.model.State;
import com.netcracker.cloud.bluegreen.api.model.Version;
import com.netcracker.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import com.netcracker.cloud.bluegreen.impl.util.EnvUtil;
import com.netcracker.cloud.maas.bluegreen.kafka.BGKafkaConsumer;
import com.netcracker.cloud.maas.bluegreen.kafka.CommitMarker;
import com.netcracker.cloud.maas.bluegreen.kafka.Record;
import com.netcracker.cloud.maas.bluegreen.kafka.RecordsBatch;
import com.netcracker.maas.declarative.kafka.client.api.exception.MaasKafkaIllegalStateException;
import com.netcracker.cloud.maas.client.api.kafka.TopicAddress;
import com.netcracker.maas.declarative.kafka.client.SyncBarrier;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaConsumerErrorHandler;
import com.netcracker.maas.declarative.kafka.client.impl.client.consumer.DeserializerHolder;
import com.netcracker.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class MaasConsumingExecutorTest {
    ConsumerExecContext ctx;
    RecordsGenerator recordsGenerator;

    @BeforeEach
    void setup() {
        System.setProperty(EnvUtil.NAMESPACE_PROP, "cloud-dev");

        ctx = new ConsumerExecContext();
        ctx.setAwaitAfterErrorTimeList(List.of(100L));
        ctx.setDeserializerHolder(new DeserializerHolder(new StringDeserializer(), new StringDeserializer()));
        ctx.setPollDuration(Duration.ofSeconds(1));

        var topicAddress = mock(TopicAddress.class);
        when(topicAddress.getTopicName()).thenReturn("orders");
        ctx.setTopic(topicAddress);
        ctx.setExecutorService(new ScheduledThreadPoolExecutor(10));

        recordsGenerator = new RecordsGenerator();
    }

    @AfterEach
    void tearDown() {
        ctx.getExecutorService().shutdown();
    }

    @Test
    void testNormalRun() {
        var consumer = mock(BGKafkaConsumer.class);
        var barrier = new SyncBarrier();
        when(consumer.poll(any())).thenAnswer(inv -> {
            barrier.notify("polled");
            return recordsGenerator.next();
        });

        var consumerCreatorService = mock(KafkaClientCreationService.class);
        when(consumerCreatorService.createKafkaConsumer(any(), any(), any(), any(), any(), any())).thenReturn(consumer);
        doAnswer(i -> {
            barrier.notify("closed");
            return null;
        })
                .when(consumer).close();

        var executor = new MaasConsumingExecutor(ctx,
                (exception, errorRecord, handledRecords) -> {
                },
                consumerCreatorService,
                List.of(),
                new InMemoryBlueGreenStatePublisher());

        try {
            executor.start();
            executor.init();
            barrier.await("polled", Duration.ofSeconds(1));
        } finally {
            executor.close();
            barrier.await("closed", Duration.ofSeconds(1));
        }

        barrier.reset();

        verify(consumerCreatorService, times(1)).createKafkaConsumer(any(), any(), any(), any(), any(), any());
        verify(consumer, times(1)).close();
    }

    @Test
    void testSuspendResume() {
        var consumer = mock(BGKafkaConsumer.class);
        var barrier = new SyncBarrier();
        var consumerCreatorService = mock(KafkaClientCreationService.class);
        when(consumerCreatorService.createKafkaConsumer(any(), any(), any(), any(), any(), any())).thenReturn(consumer);

        var executor = new MaasConsumingExecutor(ctx,
                (exception, errorRecord, handledRecords) -> {
                },
                consumerCreatorService,
                List.of(),
                new InMemoryBlueGreenStatePublisher());

        when(consumer.poll(any()))
                .thenAnswer(inv -> {
                    executor.suspend();
                    return recordsGenerator.next();
                }).thenAnswer(inv -> {
                    barrier.notify("second poll");
                    return recordsGenerator.next();
                }).thenReturn(recordsGenerator.next());
        doAnswer(i -> {
            barrier.notify("closed");
            return null;
        })
                .when(consumer).close();

        try {
            executor.start();
            executor.init();
            barrier.await("closed", Duration.ofSeconds(5));
            executor.resume();
            barrier.await("second poll", Duration.ofSeconds(5));
        } finally {
            executor.close();
            barrier.await("closed", Duration.ofSeconds(5));
        }

        barrier.reset();

        verify(consumerCreatorService, times(2)).createKafkaConsumer(any(), any(), any(), any(), any(), any());
        // suspended executor should close kafka consumer
        // second call is performed in test
        verify(consumer, times(2)).close();
    }

    @Test
    void testKafkaPollingExceptionRetry() {
        var consumer = mock(BGKafkaConsumer.class);
        var barrier = new SyncBarrier();
        var consumerCreatorService = mock(KafkaClientCreationService.class);
        when(consumerCreatorService.createKafkaConsumer(any(), any(), any(), any(), any(), any())).thenReturn(consumer);

        var executor = new MaasConsumingExecutor(ctx,
                (exception, errorRecord, handledRecords) -> {
                },
                consumerCreatorService,
                List.of(),
                new InMemoryBlueGreenStatePublisher());

        when(consumer.poll(any()))
                .thenAnswer(i -> {
                    barrier.notify("consumed");
                    return recordsGenerator.next();
                })
                .thenThrow(new FencedInstanceIdException("oops"))
                .thenAnswer(i -> {
                    barrier.notify("consumed");
                    return recordsGenerator.next();
                });
        doAnswer(i -> {
            barrier.notify("closed");
            return null;
        })
                .when(consumer).close();

        try {
            executor.start();
            executor.init();
            barrier.await("consumed", Duration.ofSeconds(1));
            barrier.await("closed", Duration.ofSeconds(1));
            barrier.await("consumed", Duration.ofSeconds(1));
        } finally {
            executor.close();
        }

        barrier.reset();

        verify(consumerCreatorService, times(2)).createKafkaConsumer(any(), any(), any(), any(), any(), any());

        // there should be 2 close() method calls: one close should be performed in try/catch error handling
        // and another one in test on block finally
        verify(consumer, timeout(10_000).times(2)).close();
    }

    @Test
    void testRecordHandleException() {
        var consumer = mock(BGKafkaConsumer.class);
        var barrier = new SyncBarrier();
        var consumerCreatorService = mock(KafkaClientCreationService.class);
        when(consumerCreatorService.createKafkaConsumer(any(), any(), any(), any(), any(), any())).thenReturn(consumer);

        var recordHandler = mock(Consumer.class);
        ctx.setHandler(recordHandler);

        var executor = new MaasConsumingExecutor(ctx,
                (exception, errorRecord, handledRecords) -> barrier.notify("handled"),
                consumerCreatorService,
                List.of(),
                new InMemoryBlueGreenStatePublisher());

        when(consumer.poll(any())).thenAnswer(i -> recordsGenerator.next());

        doThrow(new RuntimeException("oops"))
                .doAnswer(i -> {
                    barrier.notify("consumed");
                    return null;
                })
                .when(recordHandler).accept(any());

        try {
            executor.start();
            executor.init();
            barrier.await("handled", Duration.ofSeconds(5));
            barrier.await("consumed", Duration.ofSeconds(5));
        } finally {
            executor.close();
        }

        barrier.reset();

        verify(consumerCreatorService, times(1)).createKafkaConsumer(any(), any(), any(), any(), any(), any());
        verify(consumer, timeout(10_000).times(1)).close();
    }

    @Test
    void testRecordErrorHandlerException() throws Exception {
        var consumer = mock(BGKafkaConsumer.class);
        var barrier = new SyncBarrier();
        var consumerCreatorService = mock(KafkaClientCreationService.class);
        var errorHandler = mock(MaasKafkaConsumerErrorHandler.class);
        when(consumerCreatorService.createKafkaConsumer(any(), any(), any(), any(), any(), any())).thenReturn(consumer);

        var recordHandler = mock(Consumer.class);
        ctx.setHandler(recordHandler);

        var executor = new MaasConsumingExecutor(
                ctx,
                errorHandler,
                consumerCreatorService,
                List.of(),
                new InMemoryBlueGreenStatePublisher());

        when(consumer.poll(any())).thenAnswer(i -> recordsGenerator.next());

        doAnswer(i -> {
            barrier.notify("consumed");
            return i;
        })
                .doThrow(new RuntimeException("oops"))
                .doAnswer(i -> {
                    barrier.notify("consumed");
                    return i;
                })
                .doNothing()
                .when(recordHandler).accept(any());

        doThrow(new RuntimeException("ouch!"))
                .when(errorHandler).handle(any(), any(), any());

        try {
            executor.start();
            executor.init();
            // record 1 processed on worker thread
            barrier.await("consumed", Duration.ofSeconds(5));
            // record 2 fails: errorHandler throws → worker halts → workerError → consumer.release() from executor thread
            verify(consumer, timeout(10_000).times(1)).close();
            // after recovery the worker resumes (halt cleared) and processes a freshly polled record
            barrier.await("consumed", Duration.ofSeconds(5));
            // wait until executor reschedules and creates a new consumer (100ms timeout + jitter)
            verify(consumerCreatorService, timeout(10_000).times(2)).createKafkaConsumer(any(), any(), any(), any(), any(), any());
        } finally {
            executor.close();
        }

        barrier.reset();

        // record 1 offset is committed synchronously after worker marks it done
        verify(consumer, timeout(10_000).atLeast(1)).commitSync(any());
        verify(consumer, timeout(10_000).times(2)).close();
    }

    @Test
    void testWorkerHaltsAfterFatalErrorNoCommitPastFailure() throws Exception {
        var consumer = mock(BGKafkaConsumer.class);
        var consumerCreatorService = mock(KafkaClientCreationService.class);
        var errorHandler = mock(MaasKafkaConsumerErrorHandler.class);
        when(consumerCreatorService.createKafkaConsumer(any(), any(), any(), any(), any(), any())).thenReturn(consumer);

        var recordHandler = mock(Consumer.class);
        ctx.setHandler(recordHandler);

        var executor = new MaasConsumingExecutor(
                ctx,
                errorHandler,
                consumerCreatorService,
                List.of(),
                new InMemoryBlueGreenStatePublisher());

        // single partition batch: offsets 0,1,2 — record at offset 1 fails fatally
        when(consumer.poll(any()))
                .thenReturn(singlePartitionBatch(3))
                .thenReturn(Optional.empty());

        // offset 0 succeeds, offset 1 throws (processing error), offset 2 must NEVER be processed
        doNothing()                                   // offset 0
                .doThrow(new RuntimeException("oops")) // offset 1
                .when(recordHandler).accept(any());

        // errorHandler rethrows → fatal → worker must halt
        doThrow(new RuntimeException("ouch!")).when(errorHandler).handle(any(), any(), any());

        try {
            executor.start();
            executor.init();

            // recovery happened: failed consumer released and a fresh one created
            verify(consumerCreatorService, timeout(10_000).times(2))
                    .createKafkaConsumer(any(), any(), any(), any(), any(), any());

            // worker stopped after the fatal record: only offsets 0 and 1 ever reached the handler,
            // offset 2 (still in the queue at failure time) was dropped, never processed
            verify(recordHandler, timeout(10_000).times(2)).accept(any());
            verify(recordHandler, after(500).times(2)).accept(any());

            // the committed offset never advanced past the failed record (offset 1 → next offset 2);
            // only offset 0 was committable, i.e. commit position 1. Nothing at position >= 3.
            verify(consumer, never()).commitSync(argThat(MaasConsumingExecutorTest::commitsPastFailure));
        } finally {
            executor.close();
        }
    }

    @Test
    void testMixedVersionsCommittedSeparately() {
        var consumer = mock(BGKafkaConsumer.class);
        var barrier = new SyncBarrier();
        var consumerCreatorService = mock(KafkaClientCreationService.class);
        when(consumerCreatorService.createKafkaConsumer(any(), any(), any(), any(), any(), any())).thenReturn(consumer);

        var recordHandler = mock(Consumer.class);
        ctx.setHandler(recordHandler);

        var v1 = new NamespaceVersion("order-processor", State.ACTIVE, new Version("v1"));
        var v2 = new NamespaceVersion("order-processor", State.CANDIDATE, new Version("v2"));

        // one poll batch holding records from two different generations on two partitions:
        // partition 0 → v1, partition 1 → v2. After the worker processes both, readyToCommit
        // holds a mixed-version snapshot, which must be committed as two separate markers.
        when(consumer.poll(any()))
                .thenReturn(twoVersionBatch(v1, v2))
                .thenAnswer(i -> {
                    barrier.notify("second-poll");
                    return Optional.empty();
                });

        var executor = new MaasConsumingExecutor(ctx,
                (exception, errorRecord, handledRecords) -> {},
                consumerCreatorService,
                List.of(),
                new InMemoryBlueGreenStatePublisher());

        try {
            executor.start();
            executor.init();
            // wait until the batch has been polled and a later iteration ran (commit happened)
            barrier.await("second-poll", Duration.ofSeconds(5));

            var captor = ArgumentCaptor.forClass(CommitMarker.class);
            verify(consumer, timeout(5_000).atLeast(2)).commitSync(captor.capture());

            var committedVersions = captor.getAllValues().stream()
                    .map(CommitMarker::getVersion)
                    .collect(java.util.stream.Collectors.toSet());
            // both generations committed under their own version — never merged into one
            org.junit.jupiter.api.Assertions.assertTrue(committedVersions.contains(v1),
                    "expected a commit for v1, got: " + committedVersions);
            org.junit.jupiter.api.Assertions.assertTrue(committedVersions.contains(v2),
                    "expected a commit for v2, got: " + committedVersions);

            // each commit marker is single-version and single-partition (no cross-version mixing)
            captor.getAllValues().forEach(marker -> {
                if (v1.equals(marker.getVersion())) {
                    org.junit.jupiter.api.Assertions.assertEquals(
                            java.util.Set.of(new TopicPartition("orders", 0)), marker.getPosition().keySet());
                } else if (v2.equals(marker.getVersion())) {
                    org.junit.jupiter.api.Assertions.assertEquals(
                            java.util.Set.of(new TopicPartition("orders", 1)), marker.getPosition().keySet());
                }
            });
        } finally {
            executor.close();
        }
    }

    /** Build a batch with two partitions stamped with different versions: orders-0 → v1, orders-1 → v2. */
    private static Optional<RecordsBatch> twoVersionBatch(NamespaceVersion v1, NamespaceVersion v2) {
        var tp0 = new TopicPartition("orders", 0);
        var tp1 = new TopicPartition("orders", 1);
        var m0 = new CommitMarker(v1, Map.of(tp0, new OffsetAndMetadata(1)));
        var m1 = new CommitMarker(v2, Map.of(tp1, new OffsetAndMetadata(1)));
        List<Record> records = new ArrayList<>();
        records.add(new Record(new ConsumerRecord("orders", 0, 0, "k0", "d0"), m0));
        records.add(new Record(new ConsumerRecord("orders", 1, 0, "k1", "d1"), m1));
        return Optional.of(new RecordsBatch(records, m1));
    }

    @Test
    void testSamePartitionTwoVersionsActiveOffsetNotDropped() throws InterruptedException {
        var consumer = mock(BGKafkaConsumer.class);
        var barrier = new SyncBarrier();
        var consumerCreatorService = mock(KafkaClientCreationService.class);
        when(consumerCreatorService.createKafkaConsumer(any(), any(), any(), any(), any(), any())).thenReturn(consumer);

        var recordHandler = mock(Consumer.class);
        ctx.setHandler(recordHandler);

        var vStale = new NamespaceVersion("order-processor", State.ACTIVE, new Version("v1"));
        var vActive = new NamespaceVersion("order-processor", State.CANDIDATE, new Version("v2"));

        // Same partition (orders-0) in flight under two generations:
        //   - stale v1 at the HIGHER offset 10 (commit position 11)
        //   - active v2 at the LOWER offset 2 (commit position 3) — e.g. v2 re-read lower after offset alignment
        // The old key-by-partition merge would keep only the higher offset (v1) and DROP the active v2 marker.
        // poll #3 blocks so no commit can run between the two worker merges, forcing both markers to coexist
        // in readyToCommit at the same time — the exact collision the (partition, version) key must survive.
        when(consumer.poll(any()))
                .thenReturn(singlePartitionVersionBatch(vStale, 10))  // poll 1: v1 p0 offset 10
                .thenReturn(singlePartitionVersionBatch(vActive, 2))  // poll 2: v2 p0 offset 2
                .thenAnswer(i -> {                                    // poll 3: block until released
                    barrier.notify("poll-blocked");
                    barrier.await("release-poll", Duration.ofSeconds(10));
                    return Optional.empty();
                })
                .thenReturn(Optional.empty());                        // poll 4+: don't block

        // first record (v1) blocks the worker until released, so v2 stays queued and no commit removes v1;
        // second record (v2) returns immediately
        doAnswer(i -> {
            barrier.notify("v1-processing");
            barrier.await("release-v1", Duration.ofSeconds(10));
            return null;
        }).doAnswer(i -> {
            barrier.notify("v2-handled");
            return null;
        }).when(recordHandler).accept(any());

        var executor = new MaasConsumingExecutor(ctx,
                (exception, errorRecord, handledRecords) -> {},
                consumerCreatorService,
                List.of(),
                new InMemoryBlueGreenStatePublisher());

        try {
            executor.start();
            executor.init();

            // worker has picked v1 and is blocked; poll #3 has blocked so no commit can run
            barrier.await("v1-processing", Duration.ofSeconds(5));
            barrier.await("poll-blocked", Duration.ofSeconds(5));

            // release v1: worker merges (p0,v1)=11, then processes v2 and merges (p0,v2)=3.
            // both now coexist in readyToCommit because the blocked poll prevents any intervening commit.
            barrier.notify("release-v1");
            barrier.await("v2-handled", Duration.ofSeconds(5));
            // the v2 notify fires inside the handler, just before the worker performs the merge — give it a moment
            Thread.sleep(300);

            // unblock the poll loop → next consume() commits the snapshot holding BOTH versions
            barrier.notify("release-poll");

            var captor = ArgumentCaptor.forClass(CommitMarker.class);
            verify(consumer, timeout(5_000).atLeast(1)).commitSync(captor.capture());

            var tp0 = new TopicPartition("orders", 0);
            // the active (lower-offset) v2 marker MUST be committed — old partition-only keying dropped it
            boolean activeCommitted = captor.getAllValues().stream().anyMatch(m ->
                    vActive.equals(m.getVersion())
                            && m.getPosition().get(tp0) != null
                            && m.getPosition().get(tp0).offset() == 3);
            org.junit.jupiter.api.Assertions.assertTrue(activeCommitted,
                    "active version offset must not be dropped; commits=" + captor.getAllValues());

            // the stale v1 marker is committed separately, never merged with v2
            boolean staleCommitted = captor.getAllValues().stream().anyMatch(m ->
                    vStale.equals(m.getVersion())
                            && m.getPosition().get(tp0) != null
                            && m.getPosition().get(tp0).offset() == 11);
            org.junit.jupiter.api.Assertions.assertTrue(staleCommitted,
                    "stale version marker should be committed separately; commits=" + captor.getAllValues());
        } finally {
            executor.close();
        }
    }

    /** Build a single-partition (orders-0), single-version batch with one record at the given offset. */
    private static Optional<RecordsBatch> singlePartitionVersionBatch(NamespaceVersion version, int offset) {
        var tp = new TopicPartition("orders", 0);
        var marker = new CommitMarker(version, Map.of(tp, new OffsetAndMetadata(offset + 1)));
        List<Record> records = new ArrayList<>();
        records.add(new Record(new ConsumerRecord("orders", 0, offset, "k" + offset, "d" + offset), marker));
        return Optional.of(new RecordsBatch(records, marker));
    }

    /** True if the marker would commit a position strictly greater than "right after the failed record" (offset 1 → pos 2). */
    private static boolean commitsPastFailure(CommitMarker marker) {
        return marker.getPosition().values().stream()
                .mapToLong(OffsetAndMetadata::offset)
                .max()
                .orElse(0) > 2;
    }

    private static Optional<RecordsBatch> nullVersionBatch() {
        var tp = new TopicPartition("orders", 0);
        var marker = new CommitMarker(null, Map.of(tp, new OffsetAndMetadata(1)));
        var record = new Record<>(new ConsumerRecord<>("orders", 0, 0, "k", "v"), marker);
        return Optional.of(new RecordsBatch<>(List.of(record), marker));
    }

    /** Build a single-partition (orders-0) batch with offsets 0..count-1 and cumulative commit markers. */
    private static Optional<RecordsBatch> singlePartitionBatch(int count) {
        var tp = new TopicPartition("orders", 0);
        var version = new NamespaceVersion("order-processor", State.ACTIVE, new Version("v1"));
        List<Record> records = new ArrayList<>(count);
        CommitMarker batchMarker = null;
        for (int i = 0; i < count; i++) {
            Map<TopicPartition, OffsetAndMetadata> pos = new HashMap<>();
            pos.put(tp, new OffsetAndMetadata(i + 1)); // commit "next offset" semantics
            CommitMarker marker = new CommitMarker(version, pos);
            records.add(new Record(new ConsumerRecord("orders", 0, i, "order" + i, "data" + i), marker));
            batchMarker = marker;
        }
        return Optional.of(new RecordsBatch(records, batchMarker));
    }

    @Test
    void testStartTwiceThrows() {
        var consumerCreatorService = mock(KafkaClientCreationService.class);
        var executor = new MaasConsumingExecutor(ctx,
                (exception, errorRecord, handledRecords) -> {},
                consumerCreatorService,
                List.of(),
                new InMemoryBlueGreenStatePublisher());
        executor.start();
        assertThrows(MaasKafkaIllegalStateException.class, executor::start);
    }

    @Test
    void testNullVersionMarkerCommitted() {
        var consumer = mock(BGKafkaConsumer.class);
        var barrier = new SyncBarrier();
        var consumerCreatorService = mock(KafkaClientCreationService.class);
        when(consumerCreatorService.createKafkaConsumer(any(), any(), any(), any(), any(), any())).thenReturn(consumer);

        var recordHandler = mock(Consumer.class);
        ctx.setHandler(recordHandler);
        doAnswer(i -> {
            barrier.notify("processed");
            return null;
        }).when(recordHandler).accept(any());

        when(consumer.poll(any()))
                .thenReturn(nullVersionBatch())
                .thenReturn(Optional.empty());

        var executor = new MaasConsumingExecutor(ctx,
                (exception, errorRecord, handledRecords) -> {},
                consumerCreatorService,
                List.of(),
                new InMemoryBlueGreenStatePublisher());

        try {
            executor.start();
            executor.init();
            barrier.await("processed", Duration.ofSeconds(10));

            var captor = ArgumentCaptor.forClass(CommitMarker.class);
            verify(consumer, timeout(10_000).atLeastOnce()).commitSync(captor.capture());
            org.junit.jupiter.api.Assertions.assertTrue(
                    captor.getAllValues().stream().anyMatch(m -> m.getVersion() == null),
                    "expected commit for null-version marker, got: " + captor.getAllValues());
        } finally {
            executor.close();
        }
    }

    @Test
    void testErrorHandlerRethrowsSameExceptionWithoutSuppressing() throws Exception {
        var consumer = mock(BGKafkaConsumer.class);
        var barrier = new SyncBarrier();
        var consumerCreatorService = mock(KafkaClientCreationService.class);
        var errorHandler = mock(MaasKafkaConsumerErrorHandler.class);
        when(consumerCreatorService.createKafkaConsumer(any(), any(), any(), any(), any(), any())).thenReturn(consumer);

        var recordHandler = mock(Consumer.class);
        ctx.setHandler(recordHandler);
        var processingError = new RuntimeException("same");

        var executor = new MaasConsumingExecutor(ctx, errorHandler, consumerCreatorService, List.of(),
                new InMemoryBlueGreenStatePublisher());

        when(consumer.poll(any())).thenAnswer(i -> singlePartitionBatch(1)).thenReturn(Optional.empty());
        doAnswer(i -> {
            barrier.notify("consumed");
            throw processingError;
        }).when(recordHandler).accept(any());
        doThrow(processingError).when(errorHandler).handle(any(), any(), any());
        doAnswer(i -> {
            barrier.notify("closed");
            return null;
        }).when(consumer).close();

        try {
            executor.start();
            executor.init();
            barrier.await("consumed", Duration.ofSeconds(10));
            verify(consumer, timeout(10_000).times(1)).close();
        } finally {
            executor.close();
            barrier.await("closed", Duration.ofSeconds(10));
        }
    }

    @Test
    void testBackpressurePause() {
        var consumer = mock(BGKafkaConsumer.class);
        var barrier = new SyncBarrier();
        var consumerCreatorService = mock(KafkaClientCreationService.class);
        when(consumerCreatorService.createKafkaConsumer(any(), any(), any(), any(), any(), any())).thenReturn(consumer);

        // max.poll.records=2 → effectiveBatch=3 → pauseThreshold=3, resumeThreshold=1, queueCapacity=9
        var configs = new HashMap<String, Object>();
        configs.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "2");
        var bgConfig = KafkaConsumerConfiguration.builder(configs).build();
        ctx.setBlueGreenConfiguration(bgConfig);

        var recordHandler = mock(Consumer.class);
        ctx.setHandler(recordHandler);

        // Worker blocks on first record until test releases it — keeps queue full above pauseThreshold
        doAnswer(i -> {
            barrier.await("release-worker", Duration.ofSeconds(5));
            return null;
        })
                .doNothing()
                .when(recordHandler).accept(any());

        // Two polls: worker pops one record from queue so by the 3rd consume() iteration
        // the queue still has 3 records (2+2-1=3) ≥ pauseThreshold(3) → pause triggered
        when(consumer.poll(any()))
                .thenAnswer(i -> recordsGenerator.next()) // poll 1: 2 records
                .thenAnswer(i -> recordsGenerator.next()) // poll 2: 2 more records while worker is blocked
                .thenReturn(Optional.empty());

        var executor = new MaasConsumingExecutor(ctx,
                (exception, errorRecord, handledRecords) -> {},
                consumerCreatorService,
                List.of(),
                new InMemoryBlueGreenStatePublisher());

        try {
            executor.start();
            executor.init();

            // after 2nd poll: queueSize(3) >= pauseThreshold(3) → pause applied
            verify(consumer, timeout(5_000).atLeastOnce()).pause();

            // release the worker — it drains the queue to 0
            barrier.notify("release-worker");

            // queueSize drops below resumeThreshold(1) → resume applied
            verify(consumer, timeout(5_000).atLeastOnce()).resume();
        } finally {
            executor.close();
        }
    }
}
