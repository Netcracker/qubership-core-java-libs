package com.netcracker.maas.declarative.kafka.client.impl.client.producer;

import com.netcracker.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import com.netcracker.cloud.bluegreen.impl.util.EnvUtil;
import com.netcracker.cloud.maas.client.api.Classifier;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicInfo;
import com.netcracker.cloud.maas.client.impl.kafka.TopicAddressImpl;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaClientFactory;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaProducer;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import com.netcracker.maas.declarative.kafka.client.api.exception.MaasKafkaException;
import com.netcracker.maas.declarative.kafka.client.api.model.MaasKafkaProducerCreationRequest;
import com.netcracker.maas.declarative.kafka.client.api.model.MaasProducerRecord;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.ManagedBy;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasTopicDefinition;
import com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.impl.ContextPropagationFilter;
import com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.impl.TracingFilter;
import com.netcracker.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationService;
import com.netcracker.maas.declarative.kafka.client.impl.client.factory.MaasKafkaClientFactoryImpl;
import com.netcracker.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import com.netcracker.maas.declarative.kafka.client.impl.common.context.propagation.DefaultContextPropagationServiceImpl;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import com.netcracker.maas.declarative.kafka.client.impl.definition.api.MaasKafkaClientDefinitionService;
import com.netcracker.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;
import com.netcracker.maas.declarative.kafka.client.impl.tracing.TracingService;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaasKafkaProducerSendSyncTimeoutTest {

    private static final String TOPIC = "orders";

    private Producer kafkaProducer;
    private KafkaClientCreationService creationService;
    private MaasKafkaClientFactory kafkaClientFactory;

    @BeforeAll
    static void setupNamespace() {
        System.setProperty(EnvUtil.NAMESPACE_PROP, "cloud-dev");
        System.setProperty("maas.client.classifier.namespace", "test_namespace");
    }

    @BeforeEach
    void setup() {
        TopicInfo topicInfo = new TopicInfo();
        topicInfo.setName(TOPIC);
        topicInfo.setAddresses(Map.of("PLAINTEXT", List.of("broker-1:9092")));
        topicInfo.setClassifier(new Classifier(TOPIC));

        MaasKafkaTopicService topicService = mock(MaasKafkaTopicService.class);
        when(topicService.getTopicAddressByDefinition(any())).thenReturn(new TopicAddressImpl(topicInfo));
        when(topicService.getTopicAddressByDefinitionAndTenantId(any(), any()))
                .thenReturn(new TopicAddressImpl(topicInfo));

        kafkaProducer = mock(Producer.class);
        creationService = mock(KafkaClientCreationService.class);
        when(creationService.createKafkaProducer(any(), any(), any())).thenReturn(kafkaProducer);

        kafkaClientFactory = new MaasKafkaClientFactoryImpl(
                mock(InternalTenantService.class),
                mock(InternalMaasTopicCredentialsExtractor.class),
                topicService,
                Collections.emptyList(),
                5,
                1000,
                new DefaultContextPropagationServiceImpl(),
                mock(MaasKafkaClientStateChangeNotificationService.class),
                mock(MaasKafkaClientDefinitionService.class),
                creationService,
                List.of(1000L),
                List.of(new ContextPropagationFilter(new DefaultContextPropagationServiceImpl()),
                        new TracingFilter(mock(TracingService.class))),
                new InMemoryBlueGreenStatePublisher("test_namespace"),
                null);
    }

    @Test
    void sendSyncTimesOutWhenKafkaNeverCompletesCallback() {
        doAnswer(invocation -> null).when(kafkaProducer).send(any(), any());

        long overrideMs = 200L;
        MaasKafkaProducer producer = activatedProducer(overrideMs, Map.of());

        long started = System.nanoTime();
        MaasKafkaException ex = assertThrows(MaasKafkaException.class, () ->
                producer.sendSync(new MaasProducerRecord<>(null, "key-1", "payload", null, null)));
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertInstanceOf(TimeoutException.class, ex.getCause());
        assertTrue(elapsedMs >= overrideMs, "elapsedMs=" + elapsedMs);
        assertTrue(elapsedMs < 5_000, "should fail quickly, elapsedMs=" + elapsedMs);
    }

    @Test
    void sendSyncDurationOverloadBoundsWaitIndependentlyOfConfig() {
        doAnswer(invocation -> null).when(kafkaProducer).send(any(), any());

        MaasKafkaProducer producer = activatedProducer(null, Map.of(
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 600_000
        ));

        long started = System.nanoTime();
        MaasKafkaException ex = assertThrows(MaasKafkaException.class, () ->
                producer.sendSync(
                        new MaasProducerRecord<>(null, "key-2", "payload", null, null),
                        Duration.ofMillis(250)));
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertInstanceOf(TimeoutException.class, ex.getCause());
        assertTrue(elapsedMs >= 250, "elapsedMs=" + elapsedMs);
        assertTrue(elapsedMs < 5_000, "should fail quickly, elapsedMs=" + elapsedMs);
    }

    @Test
    void resolveSendSyncTimeoutUsesOverrideWhenPresent() {
        MaasKafkaProducerImpl producer = (MaasKafkaProducerImpl) activatedProducer(42_000L, Map.of(
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10_000
        ));
        assertEquals(42_000L, producer.resolveSendSyncTimeoutMs());
    }

    @Test
    void resolveSendSyncTimeoutDerivesFromDeliveryTimeoutPlusSlack() {
        MaasKafkaProducerImpl producer = (MaasKafkaProducerImpl) activatedProducer(null, Map.of(
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 90_000
        ));
        assertEquals(90_000L + MaasKafkaProducerImpl.SEND_SYNC_TIMEOUT_SLACK_MS,
                producer.resolveSendSyncTimeoutMs());
    }

    @Test
    void resolveSendSyncTimeoutUsesKafkaDefaultDeliveryWhenUnset() {
        MaasKafkaProducerImpl producer = (MaasKafkaProducerImpl) activatedProducer(null, Map.of());
        assertEquals(MaasKafkaProducerImpl.DEFAULT_DELIVERY_TIMEOUT_MS
                        + MaasKafkaProducerImpl.SEND_SYNC_TIMEOUT_SLACK_MS,
                producer.resolveSendSyncTimeoutMs());
    }

    @Test
    void resolveSendSyncTimeoutAppliesFloor() {
        MaasKafkaProducerImpl producer = (MaasKafkaProducerImpl) activatedProducer(50L, Map.of());
        assertEquals(MaasKafkaProducerImpl.MIN_SEND_SYNC_TIMEOUT_MS, producer.resolveSendSyncTimeoutMs());
    }

    private MaasKafkaProducer activatedProducer(Long sendSyncTimeoutMs, Map<String, Object> kafkaClientConfig) {
        Map<String, Object> clientConfig = new HashMap<>();
        clientConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        clientConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        clientConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "broker-1:9092");
        clientConfig.putAll(kafkaClientConfig);

        MaasTopicDefinition topicDefinition = MaasTopicDefinition.builder()
                .setName(TOPIC)
                .setNamespace("test_namespace")
                .setManagedBy(ManagedBy.SELF)
                .build();
        MaasKafkaProducerDefinition definition = MaasKafkaProducerDefinition.builder()
                .setTopic(topicDefinition)
                .setTenant(false)
                .setClientConfig(clientConfig)
                .setSendSyncTimeoutMs(sendSyncTimeoutMs)
                .build();

        MaasKafkaProducer producer = kafkaClientFactory.createProducer(
                MaasKafkaProducerCreationRequest.builder()
                        .setProducerDefinition(definition)
                        .setHandler(record -> record)
                        .setKeySerializer(new StringSerializer())
                        .setValueSerializer(new StringSerializer())
                        .build());
        producer.initSync();
        producer.activateSync();
        return producer;
    }
}
