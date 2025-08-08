package com.netcracker.maas.declarative.kafka.spring.client.impl;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.maas.bluegreen.kafka.BGKafkaConsumer;
import org.qubership.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationServiceImpl;
import org.qubership.maas.declarative.kafka.client.impl.common.bg.KafkaConsumerConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.KafkaTelemetry;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.beans.factory.BeanFactory;

import java.util.Map;
import java.util.function.Function;


public class SpringKafkaClientCreationServiceImpl extends KafkaClientCreationServiceImpl {

    private final boolean tracingEnabled;
    private final BeanFactory beanFactory;
    private final MeterRegistry meterRegistry;
    private final OpenTelemetry openTelemetry;

    public SpringKafkaClientCreationServiceImpl(boolean tracingEnabled, BeanFactory beanFactory, MeterRegistry meterRegistry, OpenTelemetry openTelemetry) {
        this.tracingEnabled = tracingEnabled;
        this.beanFactory = beanFactory;
        this.meterRegistry = meterRegistry;
        this.openTelemetry = openTelemetry;
    }

    @Override
    public Producer createKafkaProducer(Map<String, Object> map, Serializer keySerializer, Serializer valueSerializer) {
        Producer producer = new KafkaProducer(map, keySerializer, valueSerializer);

        if (tracingEnabled) {
            KafkaTelemetry telemetry = KafkaTelemetry.create(openTelemetry);
            producer = telemetry.wrap(producer);
        }
        if (meterRegistry != null) {
            new KafkaClientMetrics(producer).bindTo(meterRegistry);
        }
        return producer;
    }

    @Override
    public BGKafkaConsumer<?, ?> createKafkaConsumer(KafkaConsumerConfiguration kafkaConsumerConfiguration,
                                                     Deserializer keyDeserializer,
                                                     Deserializer valueDeserializer,
                                                     String topic,
                                                     Function<Map<String, Object>, Consumer> consumerSupplier,
                                                     BlueGreenStatePublisher statePublisher) {
        Function<Map<String, Object>, Consumer> consumer = KafkaConsumer::new;
        if (meterRegistry != null) {
            consumer = config -> new KafkaConsumer<>(config, keyDeserializer, valueDeserializer) {
                private final KafkaClientMetrics kafkaClientMetrics = new KafkaClientMetrics(this);

                {
                    kafkaClientMetrics.bindTo(meterRegistry);
                }

                @Override
                public void close() {
                    super.close();
                    kafkaClientMetrics.close();
                }
            };
        }

        return super.createKafkaConsumer(
                kafkaConsumerConfiguration,
                keyDeserializer,
                valueDeserializer,
                topic,
                consumer,
                statePublisher
        );
    }
}
