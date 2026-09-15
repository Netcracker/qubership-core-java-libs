package com.netcracker.maas.declarative.kafka.client.impl.client.producer;

import com.netcracker.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import com.netcracker.cloud.bluegreen.impl.util.EnvUtil;
import com.netcracker.cloud.maas.client.api.Classifier;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicInfo;
import com.netcracker.cloud.maas.client.impl.kafka.TopicAddressImpl;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaClientFactory;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaClientState;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaProducer;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import com.netcracker.maas.declarative.kafka.client.api.model.MaasKafkaProducerCreationRequest;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.*;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Deactivating a producer whose underlying client refuses to close. The close is best effort;
 * the state is not, because a client left reporting ACTIVE can never be activated again.
 */
class MaasKafkaProducerDeactivationTest {

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
                new InMemoryBlueGreenStatePublisher("test_namespace"));
    }

    @Test
    void testDeactivationReachesInactiveEvenWhenClosingThrows() {
        doThrow(new RuntimeException("close timed out with records in flight")).when(kafkaProducer).close();

        MaasKafkaProducer producer = activatedProducer();
        assertEquals(MaasKafkaClientState.ACTIVE, producer.getClientState());

        ((MaasKafkaProducerImpl) producer).onDeactivateClientEvent();

        awaitInactive(producer);
    }

    @Test
    void testAClientDeactivatedAfterAFailedCloseCanBeActivatedAgain() {
        AtomicBoolean closeFails = new AtomicBoolean(true);
        doAnswer(invocation -> {
            if (closeFails.get()) {
                throw new RuntimeException("close timed out with records in flight");
            }
            return null;
        }).when(kafkaProducer).close();

        MaasKafkaProducer producer = activatedProducer();
        ((MaasKafkaProducerImpl) producer).onDeactivateClientEvent();
        awaitInactive(producer);

        // the activation event a switchover ends with: it is rejected on anything but
        // INITIALIZED or INACTIVE, so a client stuck in ACTIVE would never come back
        closeFails.set(false);
        producer.activateSync();

        assertEquals(MaasKafkaClientState.ACTIVE, producer.getClientState());
        verify(creationService, times(2)).createKafkaProducer(any(), any(), any());
    }

    /** Deactivation runs on the shared executor, so the state settles after the call returns. */
    private static void awaitInactive(MaasKafkaProducer producer) {
        for (int i = 0; i < 100 && producer.getClientState() != MaasKafkaClientState.INACTIVE; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertEquals(MaasKafkaClientState.INACTIVE, producer.getClientState(),
                "a deactivation that was requested has to end in INACTIVE, whatever closing did");
    }

    private MaasKafkaProducer activatedProducer() {
        MaasTopicDefinition topicDefinition = MaasTopicDefinition.builder()
                .setName(TOPIC)
                .setNamespace("test_namespace")
                .setManagedBy(ManagedBy.SELF)
                .build();
        MaasKafkaProducerDefinition definition = MaasKafkaProducerDefinition.builder()
                .setTopic(topicDefinition)
                .setTenant(false)
                .setClientConfig(Map.of(
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "broker-1:9092"))
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
