package com.netcracker.maas.declarative.kafka.spring.client.tracing;

import org.qubership.cloud.headerstracking.filters.context.RequestIdContext;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaClientFactory;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaConsumer;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaProducer;
import org.qubership.maas.declarative.kafka.client.api.model.MaasProducerRecord;
import org.qubership.maas.declarative.kafka.spring.client.KafkaTest;
import org.qubership.maas.declarative.kafka.spring.client.config.*;
import org.qubership.maas.declarative.kafka.spring.client.config.local.dev.MaasKafkaLocalDevConfig;
import org.qubership.maas.declarative.kafka.spring.client.config.local.dev.MaasKafkaLocalDevConfigProps;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Scope;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.KafkaMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TracingTests.TracingApp.class)
@TestPropertySource("classpath:application-test.yaml")
@ActiveProfiles("test")
@EnableConfigurationProperties(value = {MaasKafkaLocalDevConfigProps.class, MaasKafkaClientConfigKeeper.class})
@AutoConfigureObservability
class TracingTests extends KafkaTest {

    @Autowired
    private MaasKafkaClientFactory clientFactory;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Test
    void hasB3TracingHeaders() throws Exception {
        String topicName = UUID.randomUUID().toString();
        final CompletableFuture<ConsumerRecord<?, ?>> gotMessageFuture = new CompletableFuture<>();

        Span span = openTelemetry.getTracer("test").spanBuilder("tracing-test").startSpan();
        try (Scope ignored1 = span.makeCurrent()) {
            SpanContext spanContext = span.getSpanContext();
            try (MaasKafkaProducer producer = createAndActivateProducer(clientFactory, "test-producer", topicName)) {
                producer.sendSync(new MaasProducerRecord<>(
                        null,
                        UUID.randomUUID().toString(),
                        "test-msg",
                        null,
                        null
                ));
            }

            try (MaasKafkaConsumer ignored = createAndActivateConsumer(clientFactory, "test-consumer", topicName, gotMessageFuture::complete)) {
                ConsumerRecord<?, ?> consumerRecord = gotMessageFuture.get(30, TimeUnit.SECONDS);
                String requestId = new String(consumerRecord.headers().headers("X-Request-Id").iterator().next().value());
                String traceId = new String(consumerRecord.headers().headers("X-B3-TraceId").iterator().next().value());
                String spanId = new String(consumerRecord.headers().headers("X-B3-SpanId").iterator().next().value());
                String sampled = new String(consumerRecord.headers().headers("X-B3-Sampled").iterator().next().value());

                assertFalse(requestId.isEmpty());
                assertFalse(traceId.isEmpty());
                assertFalse(spanId.isEmpty());
                assertFalse(sampled.isEmpty());

                assertEquals(spanContext.getTraceId(), traceId);
                assertNotEquals(spanContext.getSpanId(), spanId);
            } finally {
                span.end();
            }
        }
    }

    @Test
    void requestIdIsPropagatedToHeader() throws Exception {
        String topicName = UUID.randomUUID().toString();
        final CompletableFuture<ConsumerRecord<?, ?>> gotMessageFuture = new CompletableFuture<>();
        RequestIdContext.set("test-req-id");
        try (MaasKafkaProducer producer = createAndActivateProducer(clientFactory, "test-producer", topicName)) {
            producer.sendSync(new MaasProducerRecord<>(
                    null,
                    UUID.randomUUID().toString(),
                    "test-msg",
                    null,
                    null
            ));
        }
        try (MaasKafkaConsumer ignored = createAndActivateConsumer(clientFactory, "test-consumer", topicName, gotMessageFuture::complete)) {
            ConsumerRecord<?, ?> consumerRecord = gotMessageFuture.get(30, TimeUnit.SECONDS);
            assertEquals("test-req-id", new String(consumerRecord.headers().headers("X-Request-Id").iterator().next().value()));
        }
    }

    @ImportAutoConfiguration({
            MaasKafkaProps.class,
            KafkaMetricsAutoConfiguration.class,
            MaasKafkaClientFactoryConfig.class,
            MaasKafkaLocalDevConfig.class,
            MaasKafkaCommonConfig.class,
            MaasKafkaTopicServiceConfig.class,
            org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryAutoConfiguration.class,
            org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryAutoConfiguration.class
    })
    static class TracingApp {
        static {
            System.setProperty("maas.client.classifier.namespace", "test-ns");
            System.setProperty("cloud.microservice.namespace", "test-ns");
        }
    }
}
