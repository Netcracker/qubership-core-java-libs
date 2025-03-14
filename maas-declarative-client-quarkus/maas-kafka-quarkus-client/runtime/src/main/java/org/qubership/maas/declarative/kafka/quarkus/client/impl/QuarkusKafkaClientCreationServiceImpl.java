package org.qubership.maas.declarative.kafka.quarkus.client.impl;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.maas.bluegreen.kafka.BGKafkaConsumer;
import org.qubership.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationServiceImpl;
import org.qubership.maas.declarative.kafka.client.impl.common.bg.KafkaConsumerConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.KafkaTelemetry;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;
import java.util.function.Function;

public class QuarkusKafkaClientCreationServiceImpl extends KafkaClientCreationServiceImpl {
    private final MeterRegistry registry;
    private final boolean tracingEnabled;

    public QuarkusKafkaClientCreationServiceImpl(MeterRegistry registry, Boolean tracingEnabled) {
        this.registry = registry;
        this.tracingEnabled = tracingEnabled;
    }

    @Override
    public Producer createKafkaProducer(Map<String, Object> configs, Serializer keySerializer, Serializer valueSerializer) {
        Producer kafkaProducer = super.createKafkaProducer(configs, keySerializer, valueSerializer);

        if (tracingEnabled) {
            KafkaTelemetry telemetry = KafkaTelemetry.create(GlobalOpenTelemetry.get());
            kafkaProducer = telemetry.wrap(kafkaProducer);
        }

        if (registry != null) {
            new KafkaClientMetrics(kafkaProducer).bindTo(registry);
        }
        return kafkaProducer;
    }

    @Override
    public BGKafkaConsumer<?, ?> createKafkaConsumer(
            KafkaConsumerConfiguration kafkaConsumerConfiguration,
            Deserializer keyDeserializer,
            Deserializer valueDeserializer,
            String topic,
            Function<Map<String, Object>, Consumer> consumerSupplier,
            BlueGreenStatePublisher statePublisher) {
        Function<Map<String, Object>, Consumer> consumer = KafkaConsumer::new;
        if (registry != null) {
            consumer = config -> new KafkaConsumer<>(config, keyDeserializer, valueDeserializer) {
                private final KafkaClientMetrics kafkaClientMetrics = new KafkaClientMetrics(this);

                {
                    kafkaClientMetrics.bindTo(registry);
                }

                @Override
                public void close() {
                    super.close();
                    kafkaClientMetrics.close();
                }
            };
        }

        return super.createKafkaConsumer(
                kafkaConsumerConfiguration, keyDeserializer, valueDeserializer, topic, consumer, statePublisher);
    }
}
