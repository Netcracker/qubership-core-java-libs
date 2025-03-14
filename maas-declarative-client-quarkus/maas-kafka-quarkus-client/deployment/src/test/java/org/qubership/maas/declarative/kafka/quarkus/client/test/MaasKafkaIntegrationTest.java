package org.qubership.maas.declarative.kafka.quarkus.client.test;

import org.qubership.maas.declarative.kafka.client.api.*;
import org.qubership.maas.declarative.kafka.client.api.model.MaasKafkaConsumerCreationRequest;
import org.qubership.maas.declarative.kafka.client.api.model.MaasKafkaProducerCreationRequest;
import org.qubership.maas.declarative.kafka.client.api.model.MaasProducerRecord;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import jakarta.inject.Inject;
import org.apache.commons.lang3.stream.Streams;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


public class MaasKafkaIntegrationTest extends AbstractMaasKafkaTest {

    static {
        System.setProperty("cloud.microservice.namespace", "test-ns");
        System.setProperty("maas.client.classifier.namespace", "test-ns");
    }

    private static final Logger LOG = LoggerFactory.getLogger(MaasKafkaIntegrationTest.class);

    private static class StrTestDeserializer implements Deserializer<String> {
        @Override
        public String deserialize(String topic, byte[] data) {
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    private static class StrTestSerializer implements Serializer<String> {

        @Override
        public byte[] serialize(String topic, String data) {
            return data.getBytes(StandardCharsets.UTF_8);
        }
    }

    private static final String CONSUMER_NAME = "testConsumer";
    private static final String PRODUCER_NAME = "test-producer";

    private static final String TEST_MESSAGE = "test_message";
    private static final String TEST_MESSAGE_AFTER_REACTIVATION = "test_message_after_reactivation";


    @Inject
    MaasKafkaClientFactory clientFactory;
    @Inject
    MaasKafkaClientStateManagerService eventService;

    @BeforeAll
    static void beforeAll() {
        OpenTelemetrySdk.builder()
                .setPropagators(ContextPropagators.create(B3Propagator.injectingMultiHeaders()))
                .buildAndRegisterGlobal();
    }

    @Test
    @Timeout(value = 60)
    void clientActivationAndDeactivationTest() throws Exception {
        LOG.info("starting test clientActivationAndDeactivationTest");

        final String topicFilledName = "test_topic";

        final CompletableFuture<Void> consumerDeactivationFuture = new CompletableFuture<>();
        final CompletableFuture<Void> consumerActivationFuture = new CompletableFuture<>();
        final CompletableFuture<String> consumerPollingMessageFuture = new CompletableFuture<>();
        final CompletableFuture<String> consumerPollingMessageAfterReactivationFuture = new CompletableFuture<>();

        final CompletableFuture<Void> producerDeactivationFuture = new CompletableFuture<>();
        final CompletableFuture<Void> producerActivationFuture = new CompletableFuture<>();


        Consumer<ConsumerRecord<String, String>> recordConsumer = record -> {
            LOG.info("Received record: {}", record);
            if (record.value().equals(TEST_MESSAGE)) {
                consumerPollingMessageFuture.complete(record.value());
            } else if (record.value().equals(TEST_MESSAGE_AFTER_REACTIVATION)) {
                consumerPollingMessageAfterReactivationFuture.complete(record.value());
            }
        };

        Deserializer<String> deserializer = new StrTestDeserializer();

        MaasKafkaConsumerCreationRequest consumerCreationRequest =
                MaasKafkaConsumerCreationRequest.builder()
                        .setConsumerDefinition(clientFactory.getConsumerDefinition(CONSUMER_NAME))
                        .setHandler(recordConsumer)
                        .setValueDeserializer(deserializer)
                        .build();

        Serializer<String> serializer = new StrTestSerializer();

        MaasKafkaProducerCreationRequest producerCreationRequest =
                MaasKafkaProducerCreationRequest.builder()
                        .setProducerDefinition(clientFactory.getProducerDefinition(PRODUCER_NAME))
                        .setHandler(r -> r)
                        .setValueSerializer(serializer)
                        .build();

        try (MaasKafkaConsumer consumer = clientFactory.createConsumer(consumerCreationRequest)) {
            try (MaasKafkaProducer producer = clientFactory.createProducer(producerCreationRequest)) {
                // producer watch state changing
                producer.addChangeStateListener(((oldState, newState) -> {
                    if (oldState.equals(MaasKafkaClientState.INACTIVE) && newState.equals(MaasKafkaClientState.ACTIVE)) {
                        producerActivationFuture.complete(null);
                    } else if (oldState.equals(MaasKafkaClientState.ACTIVE) && newState.equals(MaasKafkaClientState.INACTIVE)) {
                        producerDeactivationFuture.complete(null);
                    }
                }));
                // consumer watch state changing
                consumer.addChangeStateListener(((oldState, newState) -> {
                    if (oldState.equals(MaasKafkaClientState.INACTIVE) && newState.equals(MaasKafkaClientState.ACTIVE)) {
                        consumerActivationFuture.complete(null);
                    } else if (oldState.equals(MaasKafkaClientState.ACTIVE) && newState.equals(MaasKafkaClientState.INACTIVE)) {
                        consumerDeactivationFuture.complete(null);
                    }
                }));

                // initializing
                consumer.initSync();
                producer.initSync();

                assertThat(consumer.getClientState()).isEqualTo(MaasKafkaClientState.INITIALIZED);
                assertThat(producer.getClientState()).isEqualTo(MaasKafkaClientState.INITIALIZED);
                LOG.info("Successfully initialized");

                // activating
                consumer.activateSync();
                producer.activateSync();

                assertThat(consumer.getClientState()).isEqualTo(MaasKafkaClientState.ACTIVE);
                assertThat(producer.getClientState()).isEqualTo(MaasKafkaClientState.ACTIVE);
                LOG.info("Successfully activated");

                // sending message test
                MaasProducerRecord<String, String> producerRecord = new MaasProducerRecord(
                        null,
                        UUID.randomUUID().toString(),
                        TEST_MESSAGE,
                        null,
                        null
                );
                RecordMetadata recordMetadata = producer.sendSync(producerRecord);
                assertThat(recordMetadata.topic()).isEqualTo(topicFilledName);
                LOG.info("Successfully sent message to topic {}", recordMetadata.topic());

                String sentMessage = consumerPollingMessageFuture.get();
                assertThat(sentMessage).isEqualTo(TEST_MESSAGE);
                LOG.info("Successfully received message from topic {}", recordMetadata.topic());

                // deactivating using event emitter
                eventService.emitClientDeactivationEvent();
                // sync
                CompletableFuture.allOf(
                        consumerDeactivationFuture,
                        producerDeactivationFuture
                ).get();
                assertThat(consumer.getClientState()).isEqualTo(MaasKafkaClientState.INACTIVE);
                assertThat(producer.getClientState()).isEqualTo(MaasKafkaClientState.INACTIVE);
                LOG.info("Successfully deactivate all clients");

                // activation using event emitter
                eventService.emitClientActivationEvent();
                // sync
                CompletableFuture.allOf(
                        consumerActivationFuture,
                        producerActivationFuture
                ).get();
                assertThat(consumer.getClientState()).isEqualTo(MaasKafkaClientState.ACTIVE);
                assertThat(producer.getClientState()).isEqualTo(MaasKafkaClientState.ACTIVE);
                LOG.info("Successfully reactivate all clients");

                // sending message test after reactivation
                MaasProducerRecord<String, String> producerRecordAr = new MaasProducerRecord(
                        null,
                        UUID.randomUUID().toString(),
                        TEST_MESSAGE_AFTER_REACTIVATION,
                        null,
                        null
                );
                RecordMetadata recordMetadataAr = producer.sendSync(producerRecordAr);
                assertThat(recordMetadataAr.topic()).isEqualTo(topicFilledName);
                LOG.info("Successfully sent message to topic {}", recordMetadataAr.topic());

                String sentMessageAr = consumerPollingMessageAfterReactivationFuture.get();
                assertThat(sentMessageAr).isEqualTo(TEST_MESSAGE_AFTER_REACTIVATION);
                LOG.info("Successfully received message from topic {}", recordMetadataAr.topic());
            }
        }
    }

    @Test
    @Timeout(value = 60)
    void tracingTest_ProducerProvidesNewChildSpanAndInjectsIntoHeaders() throws Exception {
        final CompletableFuture<Map<String, String>> consumerPollingMessageFuture = new CompletableFuture<>();

        Consumer<ConsumerRecord<String, String>> recordConsumer = record -> {
            Map<String, String> headersMap = Streams.of(record.headers()).collect(Collectors.toMap(Header::key, header -> new String(header.value())));
            consumerPollingMessageFuture.complete(headersMap);
        };

        MaasKafkaConsumerCreationRequest consumerCreationRequest =
                MaasKafkaConsumerCreationRequest.builder()
                        .setConsumerDefinition(clientFactory.getConsumerDefinition(CONSUMER_NAME))
                        .setHandler(recordConsumer)
                        .setValueDeserializer(new StrTestDeserializer())
                        .build();

        MaasKafkaProducerCreationRequest producerCreationRequest =
                MaasKafkaProducerCreationRequest.builder()
                        .setProducerDefinition(clientFactory.getProducerDefinition(PRODUCER_NAME))
                        .setHandler(r -> r)
                        .setValueSerializer(new StrTestSerializer())
                        .build();

        try (MaasKafkaConsumer consumer = clientFactory.createConsumer(consumerCreationRequest)) {
            try (MaasKafkaProducer producer = clientFactory.createProducer(producerCreationRequest)) {

                consumer.initSync();
                producer.initSync();

                consumer.activateSync();
                producer.activateSync();

                Span span = GlobalOpenTelemetry.get().getTracer("test").spanBuilder("tracing-test").startSpan();
                try (Scope ignored = span.makeCurrent()) {
                    String spanId = span.getSpanContext().getSpanId();

                    LOG.info("spanId {}", spanId);

                    MaasProducerRecord<String, String> producerRecord = new MaasProducerRecord(
                            null,
                            UUID.randomUUID().toString(),
                            TEST_MESSAGE,
                            null,
                            null
                    );

                    producer.sendSync(producerRecord);
                    assertEquals(spanId, Span.current().getSpanContext().getSpanId());

                    Map<String, String> headersInConsumer = consumerPollingMessageFuture.get();
                    LOG.info("map of headers from recordConsumer {}", headersInConsumer);
                    assertNotEquals(spanId, headersInConsumer.get("X-B3-SpanId"));
                } finally {
                    span.end();
                }
            }
        }
    }

    @Test
    @Timeout(value = 60)
    @Disabled("hangs sometimes")
    void tracingTest_Consumer() throws Exception {
        final CompletableFuture<Map<String, String>> consumerPollingMessageFuture = new CompletableFuture<>();

        Consumer<ConsumerRecord<String, String>> recordConsumer = record -> {
            Map<String, String> headersMap = Streams.of(record.headers()).collect(Collectors.toMap(Header::key, header -> new String(header.value())));
            consumerPollingMessageFuture.complete(headersMap);
        };

        MaasKafkaConsumerCreationRequest consumerCreationRequest =
                MaasKafkaConsumerCreationRequest.builder()
                        .setConsumerDefinition(clientFactory.getConsumerDefinition(CONSUMER_NAME))
                        .setHandler(recordConsumer)
                        .setValueDeserializer(new StrTestDeserializer())
                        .build();

        MaasKafkaProducerCreationRequest producerCreationRequest =
                MaasKafkaProducerCreationRequest.builder()
                        .setProducerDefinition(clientFactory.getProducerDefinition(PRODUCER_NAME))
                        .setHandler(r -> r)
                        .setValueSerializer(new StrTestSerializer())
                        .build();

        try (MaasKafkaConsumer consumer = clientFactory.createConsumer(consumerCreationRequest)) {
            try (MaasKafkaProducer producer = clientFactory.createProducer(producerCreationRequest)) {

                consumer.initSync();
                producer.initSync();

                consumer.activateSync();
                producer.activateSync();

                MaasProducerRecord<String, String> producerRecord = new MaasProducerRecord(
                        null,
                        UUID.randomUUID().toString(),
                        TEST_MESSAGE,
                        null,
                        null
                );
                Span span = GlobalOpenTelemetry.get().getTracer("test").spanBuilder("tracing-test").startSpan();
                try (Scope ignored = span.makeCurrent()) {
                    SpanContext spanContext = span.getSpanContext();
                    producer.sendSync(producerRecord);
                    Map<String, String> headers = consumerPollingMessageFuture.get();

                    assertEquals(spanContext.getTraceId(), headers.get("X-B3-TraceId"));
                    assertNotEquals(spanContext.getSpanId(), headers.get("X-B3-SpanId"));
                } finally {
                    span.end();
                }
            }
        }
    }
}
