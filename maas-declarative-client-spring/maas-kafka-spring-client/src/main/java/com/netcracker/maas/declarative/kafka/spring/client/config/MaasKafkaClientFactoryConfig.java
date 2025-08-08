package org.qubership.maas.declarative.kafka.spring.client.config;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

import static org.qubership.maas.declarative.kafka.client.impl.common.constant.MaasKafkaConsumerConstants.DEFAULT_AWAIT_TIME_LIST;

@Configuration
public class MaasKafkaClientFactoryConfig {

    @Autowired
    InternalTenantService tenantService;
    @Autowired
    InternalMaasTopicCredentialsExtractor credentialsExtractor;
    @Autowired
    ContextPropagationService contextPropagationService;
    @Autowired
    MaasKafkaClientStateChangeNotificationService clientStateChangeNotificationService;
    @Autowired
    MaasKafkaClientDefinitionService clientDefinitionService;
    @Autowired
    KafkaClientCreationService kafkaClientCreationService;
    @Autowired
    MaasKafkaTopicService maasKafkaTopicService;
    @Autowired
    MaasKafkaProps maasKafkaProps;
    @Autowired
    BlueGreenStatePublisher statePublisher;


    @Bean
    MaasKafkaClientFactory maasKafkaClientFactory(List<ConsumerRecordFilter> consumerRecordFilters) {
        return new MaasKafkaClientFactoryImpl(
                tenantService,
                credentialsExtractor,
                maasKafkaTopicService,
                maasKafkaProps.acceptableTenants == null ? Collections.emptyList() : maasKafkaProps.acceptableTenants,
                maasKafkaProps.consumerThreadPoolSize,
                maasKafkaProps.consumerCommonPoolDuration,
                contextPropagationService,
                clientStateChangeNotificationService,
                clientDefinitionService,
                kafkaClientCreationService,
                maasKafkaProps.awaitTimeoutAfterError == null ? DEFAULT_AWAIT_TIME_LIST : maasKafkaProps.awaitTimeoutAfterError,
                consumerRecordFilters,
                statePublisher
        );
    }

}
