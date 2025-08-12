package com.netcracker.maas.declarative.kafka.client.impl.client;

import com.netcracker.cloud.maas.client.api.kafka.TopicAddress;
import com.netcracker.maas.declarative.kafka.client.OncePutMap;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import com.netcracker.maas.declarative.kafka.client.api.context.propagation.ContextPropagationService;
import com.netcracker.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationService;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasKafkaCommonClientDefinition;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasTopicDefinition;
import com.netcracker.maas.declarative.kafka.client.impl.client.common.MaasTopicWrap;
import com.netcracker.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import com.netcracker.maas.declarative.kafka.client.impl.client.producer.MaasKafkaProducerImpl;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import com.netcracker.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import static com.netcracker.maas.declarative.kafka.client.api.model.definition.ManagedBy.SELF;
import static org.assertj.core.api.Assertions.assertThat;

public class ProducerTenantsChangingTest {

    private static class TestableProducer extends MaasKafkaProducerImpl {

        public TestableProducer(
                InternalTenantService tenantService,
                MaasKafkaTopicService kafkaTopicService,
                InternalMaasTopicCredentialsExtractor credentialsExtractor,
                MaasKafkaCommonClientDefinition clientDefinition,
                List<String> acceptableTenants,
                Map<String, MaasTopicWrap> topicMap,
                ContextPropagationService contextPropagationService,
                MaasKafkaClientStateChangeNotificationService notificationService,
                KafkaClientCreationService clientCreationService
        ) {
            super(
                    tenantService,
                    kafkaTopicService,
                    credentialsExtractor,
                    clientDefinition,
                    acceptableTenants,
                    topicMap,
                    contextPropagationService,
                    notificationService,
                    clientCreationService,
                    null
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
        MaasKafkaCommonClientDefinition clientDefinition = MaasKafkaProducerDefinition.builder()
                .setTopic(testTopicDefinition)
                .setTenant(true)
                .build();

        TestableProducer producer = new TestableProducer(
                Mockito.mock(InternalTenantService.class),
                Mockito.mock(MaasKafkaTopicService.class),
                Mockito.mock(InternalMaasTopicCredentialsExtractor.class),
                clientDefinition,
                Arrays.asList(MAIN_TENANT, TEMPORAL_TENANT),
                new OncePutMap<>(),
                Mockito.mock(ContextPropagationService.class),
                Mockito.mock(MaasKafkaClientStateChangeNotificationService.class),
                Mockito.mock(KafkaClientCreationService.class)
        );

        assertThat(producer.containsTenantTopic(MAIN_TENANT)).isFalse();
        assertThat(producer.containsTenantTopic(TEMPORAL_TENANT)).isFalse();

        // call twice for also test idempotency
        producer.newActiveTenantEvent(Arrays.asList(MAIN_TENANT, TEMPORAL_TENANT));
        producer.newActiveTenantEvent(Arrays.asList(MAIN_TENANT, TEMPORAL_TENANT));

        assertThat(producer.containsTenantTopic(MAIN_TENANT)).isTrue();
        assertThat(producer.containsTenantTopic(TEMPORAL_TENANT)).isTrue();

        // remove tenant
        producer.newActiveTenantEvent(Arrays.asList(MAIN_TENANT));
        producer.newActiveTenantEvent(Arrays.asList(MAIN_TENANT));

        assertThat(producer.containsTenantTopic(MAIN_TENANT)).isTrue();
        assertThat(producer.containsTenantTopic(TEMPORAL_TENANT)).isFalse();

        // test for acceptable tenants list
        String notAcceptableTenant = UUID.randomUUID().toString();
        producer.newActiveTenantEvent(Arrays.asList(MAIN_TENANT, notAcceptableTenant));

        assertThat(producer.containsTenantTopic(MAIN_TENANT)).isTrue();
        assertThat(producer.containsTenantTopic(notAcceptableTenant)).isFalse();
    }

}
