package org.qubership.maas.declarative.kafka.quarkus.client.config;


import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaClientFactory;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import org.qubership.maas.declarative.kafka.client.api.context.propagation.ContextPropagationService;
import org.qubership.maas.declarative.kafka.client.api.filter.ConsumerRecordFilter;
import org.qubership.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationService;
import org.qubership.maas.declarative.kafka.client.impl.client.factory.MaasKafkaClientFactoryImpl;
import org.qubership.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import org.qubership.maas.declarative.kafka.client.impl.definition.api.MaasKafkaClientDefinitionService;
import org.qubership.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collections;

import static org.qubership.maas.declarative.kafka.client.impl.common.constant.MaasKafkaConsumerConstants.DEFAULT_AWAIT_TIME_LIST;


@Singleton
public class MaasKafkaClientFactoryConfig {

    @Inject
    InternalTenantService tenantService;
    @Inject
    InternalMaasTopicCredentialsExtractor credentialsExtractor;
    @Inject
    ContextPropagationService contextPropagationService;
    @Inject
    MaasKafkaClientStateChangeNotificationService clientStateChangeNotificationService;
    @Inject
    MaasKafkaClientDefinitionService clientDefinitionService;
    @Inject
    KafkaClientCreationService kafkaClientCreationService;
    @Inject
    MaasKafkaTopicService maasKafkaTopicService;
    @Inject
    MaasKafkaProps maasKafkaProps;
    @Inject
    BlueGreenStatePublisher statePublisher;

    @Singleton
    @Produces
    MaasKafkaClientFactory maasKafkaClientFactory(
            Instance<ConsumerRecordFilter> consumerRecordFilters
    ) {
        return new MaasKafkaClientFactoryImpl(
                tenantService,
                credentialsExtractor,
                maasKafkaTopicService,
                maasKafkaProps.acceptableTenants.orElse(Collections.emptyList()),
                maasKafkaProps.consumerThreadPoolSize,
                maasKafkaProps.consumerCommonPoolDuration,
                contextPropagationService,
                clientStateChangeNotificationService,
                clientDefinitionService,
                kafkaClientCreationService,
                maasKafkaProps.awaitItmeoutAfterError.orElse(DEFAULT_AWAIT_TIME_LIST),
                consumerRecordFilters.stream().toList(),
                statePublisher
        );
    }

}
