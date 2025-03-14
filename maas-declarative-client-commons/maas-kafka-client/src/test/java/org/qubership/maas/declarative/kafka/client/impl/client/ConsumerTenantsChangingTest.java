package org.qubership.maas.declarative.kafka.client.impl.client;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.maas.declarative.kafka.client.OncePutMap;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaConsumerErrorHandler;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaConsumerDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasTopicDefinition;
import org.qubership.maas.declarative.kafka.client.impl.client.common.MaasTopicWrap;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.MaasKafkaConsumerImpl;
import org.qubership.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationService;
import org.qubership.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import org.qubership.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.qubership.maas.declarative.kafka.client.api.model.definition.ManagedBy.SELF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ConsumerTenantsChangingTest {

    private static class TestableConsumer extends MaasKafkaConsumerImpl {

        public <K, V> TestableConsumer(
                InternalTenantService tenantService,
                MaasKafkaTopicService kafkaTopicService,
                InternalMaasTopicCredentialsExtractor credentialsExtractor,
                MaasKafkaConsumerDefinition consumerDefinition,
                Consumer<ConsumerRecord<K, V>> handler,
                MaasKafkaConsumerErrorHandler errorHandler,
                List<String> acceptableTenants,
                ScheduledExecutorService executorService,
                Integer commonPoolDuration,
                Map<String, MaasTopicWrap> topicMap,
                MaasKafkaClientStateChangeNotificationService notificationService,
                KafkaClientCreationService clientCreationService,
                BlueGreenStatePublisher statePublisher
        ) {
            super(
                    tenantService,
                    kafkaTopicService,
                    credentialsExtractor,
                    consumerDefinition,
                    handler,
                    errorHandler,
                    acceptableTenants,
                    executorService,
                    commonPoolDuration,
                    topicMap,
                    notificationService,
                    clientCreationService,
                    null,
                    statePublisher
            );
        }

        @Override
        protected void createNewTenantTopicClient(String tenantId, BiConsumer<String, TopicAddress> creator) {
            // Ignore
        }

        boolean containsTenantTopic(String tenantId) {
            return topicMap.containsKey(tenantId);
        }

    }

    @Test
    void tenantsChangingTest() {
        final String MAIN_TENANT = UUID.randomUUID().toString();
        final String TEMPORAL_TENANT = UUID.randomUUID().toString();
        MaasTopicDefinition testTopicDefinition = MaasTopicDefinition.builder()
                .setName("test_name")
                .setNamespace("test_namespace")
                .setManagedBy(SELF)
                .build();
        MaasKafkaConsumerDefinition clientDefinition = MaasKafkaConsumerDefinition.builder()
                .setTopic(testTopicDefinition)
                .setTenant(true)
                .setInstanceCount(1)
                .setPollDuration(1)
                .setGroupId("test-group")
                .build();

        TestableConsumer consumer = new TestableConsumer(
                mock(InternalTenantService.class),
                mock(MaasKafkaTopicService.class),
                mock(InternalMaasTopicCredentialsExtractor.class),
                clientDefinition,
                null,
                null,
                Arrays.asList(MAIN_TENANT, TEMPORAL_TENANT),
                null,
                1,
                new OncePutMap<>(),
                mock(MaasKafkaClientStateChangeNotificationService.class),
                mock(KafkaClientCreationService.class),
                mock(BlueGreenStatePublisher.class)
        );

        assertThat(consumer.containsTenantTopic(MAIN_TENANT)).isFalse();
        assertThat(consumer.containsTenantTopic(TEMPORAL_TENANT)).isFalse();

        // call twice for also test idempotency
        consumer.newActiveTenantEvent(Arrays.asList(MAIN_TENANT, TEMPORAL_TENANT));
        consumer.newActiveTenantEvent(Arrays.asList(MAIN_TENANT, TEMPORAL_TENANT));

        assertThat(consumer.containsTenantTopic(MAIN_TENANT)).isTrue();
        assertThat(consumer.containsTenantTopic(TEMPORAL_TENANT)).isTrue();

        // remove tenant
        consumer.newActiveTenantEvent(Arrays.asList(MAIN_TENANT));
        consumer.newActiveTenantEvent(Arrays.asList(MAIN_TENANT));

        assertThat(consumer.containsTenantTopic(MAIN_TENANT)).isTrue();
        assertThat(consumer.containsTenantTopic(TEMPORAL_TENANT)).isFalse();

        // test for acceptable tenants list
        String notAcceptableTenant = UUID.randomUUID().toString();
        consumer.newActiveTenantEvent(Arrays.asList(MAIN_TENANT, notAcceptableTenant));

        assertThat(consumer.containsTenantTopic(MAIN_TENANT)).isTrue();
        assertThat(consumer.containsTenantTopic(notAcceptableTenant)).isFalse();
    }

}
