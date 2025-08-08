package org.qubership.maas.declarative.kafka.client.impl.client.consumer.executor;

import org.apache.kafka.common.errors.FencedInstanceIdException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.impl.util.EnvUtil;
import org.qubership.cloud.maas.bluegreen.kafka.BGKafkaConsumer;
import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.maas.declarative.kafka.client.SyncBarrier;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaConsumerErrorHandler;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.DeserializerHolder;
import org.qubership.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationService;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
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
            barrier.await("handled", Duration.ofSeconds(1));
            barrier.await("consumed", Duration.ofSeconds(1));
        } finally {
            executor.close();
        }

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
            barrier.await("consumed", Duration.ofSeconds(1));
            verify(consumer, timeout(10_000).times(1)).close();
            barrier.await("consumed", Duration.ofSeconds(1));
        } finally {
            executor.close();
        }

        verify(consumerCreatorService, times(2)).createKafkaConsumer(any(), any(), any(), any(), any(), any());
        verify(consumer, timeout(10_000).atLeast(2)).commitSync(any()); // TODO validate commit marker
        verify(consumer, timeout(10_000).times(2)).close();
    }
}
