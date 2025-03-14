package org.qubership.maas.declarative.kafka.client.impl.client.factory;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.maas.client.impl.Lazy;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaClientFactory;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaConsumer;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaProducer;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import org.qubership.maas.declarative.kafka.client.api.context.propagation.ContextPropagationService;
import org.qubership.maas.declarative.kafka.client.api.filter.ConsumerRecordFilter;
import org.qubership.maas.declarative.kafka.client.api.model.MaasKafkaConsumerCreationRequest;
import org.qubership.maas.declarative.kafka.client.api.model.MaasKafkaProducerCreationRequest;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaConsumerDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.MaasKafkaConsumerImpl;
import org.qubership.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationService;
import org.qubership.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import org.qubership.maas.declarative.kafka.client.impl.client.producer.MaasKafkaProducerImpl;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import org.qubership.maas.declarative.kafka.client.impl.definition.api.MaasKafkaClientDefinitionService;
import org.qubership.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

public class MaasKafkaClientFactoryImpl implements MaasKafkaClientFactory {
    private final InternalTenantService tenantService;
    private final InternalMaasTopicCredentialsExtractor topicCredentialsExtractor;
    private final MaasKafkaTopicService kafkaTopicService;
    private final List<String> acceptableTenants;

    private final Map<String, MaasKafkaConsumerDefinition> consumerDefinitionMap = new ConcurrentHashMap<>();
    private final Map<String, MaasKafkaProducerDefinition> producerDefinitionMap = new ConcurrentHashMap<>();

    private final int consumerCommonPoolDuration;
    private final List<Long> awaitAfterErrorOccurred;

    private final ContextPropagationService contextPropagationService;
    private final MaasKafkaClientStateChangeNotificationService maasKafkaClientStateChangeNotificationService;
    private final MaasKafkaClientDefinitionService maasKafkaClientDefinitionService;
    private final KafkaClientCreationService kafkaClientCreationService;
    private final List<ConsumerRecordFilter> recordFilters;
    private final BlueGreenStatePublisher statePublisher;
    private final Lazy<ScheduledExecutorService> sharedConsumerExecutorServiceHolder;

    public MaasKafkaClientFactoryImpl(
            InternalTenantService tenantService,
            InternalMaasTopicCredentialsExtractor topicCredentialsExtractor,
            MaasKafkaTopicService kafkaTopicService,
            List<String> acceptableTenants,
            Integer threadPoolSize,
            int consumerCommonPoolDuration,
            ContextPropagationService contextPropagationService,
            MaasKafkaClientStateChangeNotificationService maasKafkaClientStateChangeNotificationService,
            MaasKafkaClientDefinitionService maasKafkaClientDefinitionService,
            KafkaClientCreationService kafkaClientCreationService,
            List<Long> awaitAfterErrorOccurred,
            List<ConsumerRecordFilter> recordFilters,
            BlueGreenStatePublisher statePublisher
    ) {
        this.tenantService = tenantService;
        this.topicCredentialsExtractor = topicCredentialsExtractor;
        this.kafkaTopicService = kafkaTopicService;
        this.acceptableTenants = prepareTenants(acceptableTenants);
        this.consumerCommonPoolDuration = consumerCommonPoolDuration;
        this.contextPropagationService = contextPropagationService;
        this.maasKafkaClientStateChangeNotificationService = maasKafkaClientStateChangeNotificationService;
        this.maasKafkaClientDefinitionService = maasKafkaClientDefinitionService;
        this.kafkaClientCreationService = kafkaClientCreationService;
        this.awaitAfterErrorOccurred = awaitAfterErrorOccurred;
        this.recordFilters = recordFilters;
        this.statePublisher = statePublisher;
        this.sharedConsumerExecutorServiceHolder = new Lazy<>(() ->
                Executors.newScheduledThreadPool(threadPoolSize, createThreadFactory("maas-kafka-exec-%s")));
    }

    @Override
    public MaasKafkaConsumerDefinition getConsumerDefinition(String consumerName) {
        return consumerDefinitionMap.computeIfAbsent(consumerName, maasKafkaClientDefinitionService::getConsumerDefinition);
    }

    @Override
    public MaasKafkaProducerDefinition getProducerDefinition(String producerName) {
        return producerDefinitionMap.computeIfAbsent(producerName, maasKafkaClientDefinitionService::getProducerDefinition);
    }

    @Override
    public MaasKafkaConsumer createConsumer(MaasKafkaConsumerCreationRequest consumerCreationRequest) {
        ScheduledExecutorService consumerExecutorService;
        if (consumerCreationRequest.getConsumerDefinition().getDedicatedThreadPoolSize() != null) {
            String topicNamedPattern = "maas-kafka-exec-%s-%%d".formatted(consumerCreationRequest.getConsumerDefinition().getTopic().getName());
            consumerExecutorService = Executors.newScheduledThreadPool(
                    consumerCreationRequest.getConsumerDefinition().getDedicatedThreadPoolSize(),
                    createThreadFactory(topicNamedPattern)
            );
        } else {
            consumerExecutorService = sharedConsumerExecutorServiceHolder.get();
        }

        return new MaasKafkaConsumerImpl(
                consumerCreationRequest,
                tenantService,
                kafkaTopicService,
                topicCredentialsExtractor,
                acceptableTenants,
                consumerExecutorService,
                consumerCommonPoolDuration,
                maasKafkaClientStateChangeNotificationService,
                kafkaClientCreationService,
                awaitAfterErrorOccurred,
                recordFilters,
                statePublisher
        );
    }

    @Override
    public MaasKafkaProducer createProducer(MaasKafkaProducerCreationRequest producerCreationRequest) {
        return new MaasKafkaProducerImpl(
                producerCreationRequest,
                tenantService,
                kafkaTopicService,
                topicCredentialsExtractor,
                acceptableTenants,
                contextPropagationService,
                maasKafkaClientStateChangeNotificationService,
                kafkaClientCreationService
        );
    }


    private static List<String> prepareTenants(List<String> acceptableTenants) {
        if (!acceptableTenants.isEmpty()) {
            return acceptableTenants.stream()
                    .map(UUID::fromString)// UUID validation
                    .map(Objects::toString)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static ThreadFactory createThreadFactory(String pattern) {
        return new BasicThreadFactory.Builder()
                .namingPattern(pattern)
                .daemon(true)
                .build();
    }
}
