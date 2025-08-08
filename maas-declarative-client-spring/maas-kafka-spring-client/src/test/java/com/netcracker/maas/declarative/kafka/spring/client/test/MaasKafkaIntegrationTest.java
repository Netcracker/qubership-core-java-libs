package com.netcracker.maas.declarative.kafka.spring.client.test;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.qubership.cloud.security.core.auth.M2MManager;
import org.qubership.maas.declarative.kafka.client.api.*;
import org.qubership.maas.declarative.kafka.client.api.model.MaasProducerRecord;
import org.qubership.maas.declarative.kafka.spring.client.KafkaTest;
import org.qubership.maas.declarative.kafka.spring.client.config.*;
import org.qubership.maas.declarative.kafka.spring.client.config.local.dev.MaasKafkaLocalDevConfig;
import org.qubership.maas.declarative.kafka.spring.client.config.local.dev.MaasKafkaLocalDevConfigProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = {
        MaasKafkaLocalDevConfig.class,
        MaasKafkaClientFactoryConfig.class,
        MaasKafkaClientStateManagerConfig.class,
        MaasKafkaCommonConfig.class,
        MaasKafkaProps.class,
        MaasKafkaTopicServiceConfig.class,
        OpenTelemetryAutoConfiguration.class
})
@EnableConfigurationProperties(value = {MaasKafkaLocalDevConfigProps.class, MaasKafkaClientConfigKeeper.class})
@TestPropertySource("classpath:application-test.yaml")
@ActiveProfiles("test")
@AutoConfigureObservability
class MaasKafkaIntegrationTest extends KafkaTest {

    private static final Logger LOG = LoggerFactory.getLogger(MaasKafkaIntegrationTest.class);

    private static final String CONSUMER_NAME = "test-consumer";
    private static final String PRODUCER_NAME = "test-producer";

    private static final String TEST_MESSAGE = "test_message";
    private static final String TEST_MESSAGE_AFTER_REACTIVATION = "test_message_after_reactivation";


    @Autowired
    MaasKafkaClientFactory clientFactory;
    @Autowired
    MaasKafkaClientStateManagerService eventService;
    @MockBean
    M2MManager m2MManager;

    @Test
    @Timeout(value = 120)
    void clientActivationAndDeactivationTest() throws Exception {
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

        MaasKafkaConsumer consumer = createConsumer(clientFactory, CONSUMER_NAME, recordConsumer);
        MaasKafkaProducer producer = createProducer(clientFactory, PRODUCER_NAME);

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

        producer.close();
        consumer.close();
    }
}
