package org.qubership.maas.declarative.kafka.spring.client.monitoring;

import org.qubership.maas.declarative.kafka.client.api.MaasKafkaClientFactory;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaConsumer;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaProducer;
import org.qubership.maas.declarative.kafka.client.api.model.MaasProducerRecord;
import org.qubership.maas.declarative.kafka.spring.client.KafkaTest;
import org.qubership.maas.declarative.kafka.spring.client.config.*;
import org.qubership.maas.declarative.kafka.spring.client.config.local.dev.MaasKafkaLocalDevConfig;
import org.qubership.maas.declarative.kafka.spring.client.config.local.dev.MaasKafkaLocalDevConfigProps;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.KafkaMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = MonitoringTests.MetricsApp.class)
@TestPropertySource("classpath:application-test.yaml")
@ActiveProfiles("test")
@EnableConfigurationProperties(value = {MaasKafkaLocalDevConfigProps.class, MaasKafkaClientConfigKeeper.class})
@AutoConfigureObservability
class MonitoringTests extends KafkaTest {

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private MaasKafkaClientFactory clientFactory;

    @Test
    void consumerAndProducerAreInstrumented() throws Exception {
        String topicName = UUID.randomUUID().toString();
        final CompletableFuture<ConsumerRecord<?, ?>> gotMessageFuture = new CompletableFuture<>();
        try (MaasKafkaProducer producer = createAndActivateProducer(clientFactory, "test-producer", topicName)) {
            producer.sendSync(new MaasProducerRecord<>(
                    null,
                    UUID.randomUUID().toString(),
                    "test-msg",
                    null,
                    null
            ));

            try (MaasKafkaConsumer ignored = createAndActivateConsumer(clientFactory, "test-consumer", topicName, gotMessageFuture::complete)) {
                gotMessageFuture.get(30, TimeUnit.SECONDS);

                assertNotNull(registry.getMeters());
                assertFalse(registry.getMeters().isEmpty());
                assertThat(this.registry.get("kafka.consumer.fetch.manager.records.consumed.total").functionCounter().count())
                        .isPositive();
                assertThat(this.registry.get("kafka.producer.record.send.total").functionCounter().count())
                        .isPositive();
            }
        }
    }

    @ImportAutoConfiguration({
            MaasKafkaProps.class,
            KafkaMetricsAutoConfiguration.class,
            MaasKafkaClientFactoryConfig.class,
            MaasKafkaLocalDevConfig.class,
            MaasKafkaCommonConfig.class,
            MaasKafkaTopicServiceConfig.class,
            OpenTelemetryAutoConfiguration.class
    })
    static class MetricsApp {
        static {
            System.setProperty("maas.client.classifier.namespace", "test-ns");
            System.setProperty("cloud.microservice.namespace", "test-ns");
        }

        @Primary
        @Bean
        MeterRegistry registry() {
            return new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());
        }
    }
}