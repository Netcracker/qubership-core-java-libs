package com.netcracker.maas.declarative.kafka.client.impl.client.consumer;

import org.qubership.cloud.framework.contexts.tenant.context.TenantContext;
import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaClientState;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaConsumer;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaConsumerErrorHandler;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import org.qubership.maas.declarative.kafka.client.api.filter.ConsumerRecordFilter;
import org.qubership.maas.declarative.kafka.client.api.model.MaasKafkaConsumerCreationRequest;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaBlueGreenDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaConsumerDefinition;
import org.qubership.maas.declarative.kafka.client.impl.client.common.MaasKafkaCommonClient;
import org.qubership.maas.declarative.kafka.client.impl.client.common.MaasTopicWrap;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.errorhandling.impl.DefaultConsumerHandlerImpl;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.executor.ConsumerExecContext;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.executor.MaasConsumingExecutor;
import org.qubership.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationService;
import org.qubership.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import org.qubership.maas.declarative.kafka.client.impl.common.bg.KafkaConsumerConfiguration;
import org.qubership.maas.declarative.kafka.client.impl.common.constant.MaasKafkaConsumerConstants;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import org.qubership.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import static org.qubership.maas.declarative.kafka.client.impl.Utils.safe;
import static org.apache.kafka.clients.CommonClientConfigs.GROUP_ID_CONFIG;

public class MaasKafkaConsumerImpl extends MaasKafkaCommonClient implements MaasKafkaConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MaasKafkaConsumerImpl.class);

    private final boolean customProcessed;
    // Non tenant
    private List<ConsumerExecContext> consumerWorkerContext;
    // Custom handling enabled
    private final Map<String, List<ConsumerExecContext>> consumerWorkerContextMap = new ConcurrentHashMap<>();
    private java.util.function.Consumer handler;
    private MaasKafkaConsumerErrorHandler errorHandler;
    // common worker executor
    private final ScheduledExecutorService consumerExecutorService;
    private final int commonPoolDuration;
    private DeserializerHolder deserializerHolder;

    private final int instanceCount;
    private final KafkaClientCreationService kafkaClientCreationService;
    private final List<ConsumerRecordFilter> recordFilters;

    private final List<Long> awaitAfterErrorOccurred;
    private final BlueGreenStatePublisher statePublisher;

    public MaasKafkaConsumerImpl(
            MaasKafkaConsumerCreationRequest creationRequest,
            InternalTenantService tenantService,
            MaasKafkaTopicService kafkaTopicService,
            InternalMaasTopicCredentialsExtractor credentialsExtractor,
            List<String> acceptableTenants,
            ScheduledExecutorService consumerExecutorService,
            Integer commonPoolDuration,
            MaasKafkaClientStateChangeNotificationService maasKafkaClientStateChangeNotificationService,
            KafkaClientCreationService kafkaClientCreationService,
            List<Long> awaitAfterErrorOccurred,
            List<ConsumerRecordFilter> recordFilters,
            BlueGreenStatePublisher statePublisher
    ) {
        super(
                tenantService,
                kafkaTopicService,
                credentialsExtractor,
                creationRequest.getConsumerDefinition(),
                acceptableTenants,
                maasKafkaClientStateChangeNotificationService
        );
        this.handler = creationRequest.getHandler();
        this.errorHandler = creationRequest.getErrorHandler() == null
                ? new DefaultConsumerHandlerImpl()
                : creationRequest.getErrorHandler();
        this.commonPoolDuration = commonPoolDuration;
        this.instanceCount = creationRequest.getConsumerDefinition().getInstanceCount();
        this.customProcessed = creationRequest.isCustomProcessed();
        this.deserializerHolder = new DeserializerHolder(
                creationRequest.getKeyDeserializer(),
                creationRequest.getValueDeserializer()
        );

        this.consumerExecutorService = consumerExecutorService;
        this.kafkaClientCreationService = kafkaClientCreationService;

        this.awaitAfterErrorOccurred = awaitAfterErrorOccurred;
        this.recordFilters = recordFilters;
        this.statePublisher = statePublisher;
    }

    // for test purposes
    protected <K, V> MaasKafkaConsumerImpl(
            InternalTenantService tenantService,
            MaasKafkaTopicService kafkaTopicService,
            InternalMaasTopicCredentialsExtractor credentialsExtractor,
            MaasKafkaConsumerDefinition clientDefinition,
            java.util.function.Consumer<ConsumerRecord<K, V>> handler,
            MaasKafkaConsumerErrorHandler errorHandler,
            List<String> acceptableTenants,
            ScheduledExecutorService consumerExecutorService,
            Integer commonPoolDuration,
            Map<String, MaasTopicWrap> topicMap,
            MaasKafkaClientStateChangeNotificationService maasKafkaClientStateChangeNotificationService,
            KafkaClientCreationService kafkaClientCreationService,
            List<ConsumerRecordFilter> recordFilters,
            BlueGreenStatePublisher statePublisher
    ) {
        super(
                tenantService,
                kafkaTopicService,
                credentialsExtractor,
                clientDefinition,
                acceptableTenants,
                topicMap,
                maasKafkaClientStateChangeNotificationService,
                null
        );
        this.handler = handler;
        this.errorHandler = errorHandler;
        this.commonPoolDuration = commonPoolDuration;
        this.instanceCount = clientDefinition.getInstanceCount();
        this.customProcessed = false;

        this.consumerExecutorService = consumerExecutorService;

        this.kafkaClientCreationService = kafkaClientCreationService;

        this.awaitAfterErrorOccurred = MaasKafkaConsumerConstants.DEFAULT_AWAIT_TIME_LIST;
        this.recordFilters = recordFilters;
        this.statePublisher = statePublisher;
    }

    @Override
    public void close() {
        if (customProcessed) {
            LOG.info("Skip destroying custom processed consumer");
            return;
        }

        try {
            if (clientDefinition.isTenant()) {
                consumerWorkerContextMap.values()
                        .forEach(cw ->
                                cw.forEach(cwcx -> safe(() -> cwcx.getExecutor().close()))
                        );
            } else {
                consumerWorkerContext.forEach(cw -> safe(() -> cw.getExecutor().close()));
            }
        } catch (Exception e) {
            LOG.error("Error destroy consumer", e);
        }
    }

    @Override
    public TopicAddress getTopic() {
        if (clientDefinition.isTenant()) {
            MaasTopicWrap topicWrap = topicMap.get(TenantContext.get());
            if (topicWrap != null) {
                return topicWrap.getTopic();
            }
            return null;
        }
        return topic;
    }

    @Override
    public boolean isCustomProcessed() {
        return customProcessed;
    }

    @Override
    public Consumer unwrap() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Consumer unwrap(String tenantId) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void activateInitialized() {
        LOG.info("Start activating maas kafka consumer: {}", clientDefinition);
        if (!customProcessed) {
            if (clientDefinition.isTenant()) {
                topicMap.forEach((tenantId, topicWrapper) -> {
                    for (int i = 0; i < instanceCount; i++) {
                        consumerWorkerContextMap.computeIfAbsent(tenantId, v -> new ArrayList<>())
                                .add(createExecContext(topicWrapper.getTopic()));
                    }
                });

                consumerWorkerContextMap
                        .forEach((key, value) -> value.forEach(exCtx -> {
                            exCtx.getExecutor().init();
                            exCtx.getExecutor().start();
                        }));
            } else {
                consumerWorkerContext = new ArrayList<>();
                for (int i = 0; i < instanceCount; i++) {
                    consumerWorkerContext.add(createExecContext(topic));
                }
                consumerWorkerContext.forEach(exCtx -> {
                    exCtx.getExecutor().init();
                    exCtx.getExecutor().start();
                });
            }
        }

        clientState = MaasKafkaClientState.ACTIVE;

        // notify changing state
        notifyStateChanging(MaasKafkaClientState.INITIALIZED, clientState);

        tenantService.subscribe(tenants -> {
            execute(() -> newActiveTenantEvent(tenants));
        });
        // Start event consuming
        startConsumingActivationEvents();
        startConsumingDeactivationEvents();
        LOG.info("Finish activating maas kafka consumer: {}", clientDefinition);
    }

    @Override
    protected void activateInactive() {
        LOG.info("Start activating maas kafka consumer: {}", clientDefinition);
        if (clientDefinition.isTenant()) {
            consumerWorkerContextMap.forEach((key, value) ->
                    value.forEach(exCtx -> exCtx.getExecutor().resume()));
        } else {
            consumerWorkerContext.forEach(exCtx -> exCtx.getExecutor().resume());
        }
        clientState = MaasKafkaClientState.ACTIVE;

        // notify changing state
        notifyStateChanging(MaasKafkaClientState.INACTIVE, clientState);
        LOG.info("Finish activating kafka consumer: {}", clientDefinition);
    }

    private ConsumerExecContext createExecContext(TopicAddress topic) {
        Map<String, Object> consumerCfg = new HashMap<>(clientDefinition.getClientConfig());
        Map<String, Object> consCfg = credentialsExtractor.extract(topic);
        consumerCfg.putAll(consCfg);

        String groupId = ((MaasKafkaConsumerDefinition) clientDefinition).getGroupId();
        consumerCfg.put(GROUP_ID_CONFIG, formatGroupId(groupId, topic.getClassifier().getTenantId().orElse("")));

        MaasKafkaConsumerDefinition consumerDefinition = (MaasKafkaConsumerDefinition) this.clientDefinition;
        ConsumerExecContext execContext = new ConsumerExecContext();
        execContext.setConnectionConfig(consumerCfg);
        execContext.setHandler(handler);
        execContext.setTopic(topic);
        execContext.setExecutorService(consumerExecutorService);
        execContext.setAwaitAfterErrorTimeList(awaitAfterErrorOccurred);

        Integer pollDuration = consumerDefinition.getPollDuration();
        if (pollDuration == null) {
            pollDuration = commonPoolDuration;
        }
        execContext.setDeserializerHolder(deserializerHolder);
        execContext.setPollDuration(Duration.ofMillis(pollDuration));

        MaasConsumingExecutor executor;
        executor = new MaasConsumingExecutor(execContext,
                errorHandler, kafkaClientCreationService, recordFilters, statePublisher);
        execContext.setExecutor(executor);

        MaasKafkaBlueGreenDefinition blueGreenDefinition = consumerDefinition.getBlueGreenDefinition();
        KafkaConsumerConfiguration.Builder kafkaConsumerConfigurationBuilder = KafkaConsumerConfiguration.builder(consumerCfg);
        if (blueGreenDefinition != null) {
            kafkaConsumerConfigurationBuilder
                    .setBlueGreen(blueGreenDefinition.isEnabled())
                    .setConsumerConsistencyMode(blueGreenDefinition.getConsumerConsistencyMode())
                    .setCandidateOffsetShift(blueGreenDefinition.getCandidateOffsetShift())
                    .setFilterEnabled(blueGreenDefinition.isFilterEnabled())
                    .setLocaldev(blueGreenDefinition.isLocaldev())
                    .setVersioned(topic.isVersioned());
        }
        execContext.setBlueGreenConfiguration(kafkaConsumerConfigurationBuilder.build());
        return execContext;
    }

    private static String formatGroupId(String groupId, String tenantId) {
        if (tenantId.isEmpty() && groupId.contains("{{tenantId}}")) {
            throw new IllegalArgumentException(String.format("group '%s' contains {{tenantId}}, but topic's classifier has no tenantId", groupId));
        }
        return groupId.replace("{{tenantId}}", tenantId);
    }

    // Methods for adding new tenant (code simplifying)
    @Override
    public void onDeactivateClientEvent() {
        execute(() -> {
            if (clientState.equals(MaasKafkaClientState.ACTIVE)) {
                LOG.info("Start deactivating maas kafka consumer: {}", clientDefinition);
                if (clientDefinition.isTenant()) {
                    consumerWorkerContextMap.forEach((key, value) ->
                            value.forEach(exCtx -> exCtx.getExecutor().suspend()));
                } else {
                    consumerWorkerContext.forEach(exCtx -> exCtx.getExecutor().suspend());
                }

                MaasKafkaClientState oldState = clientState;
                clientState = MaasKafkaClientState.INACTIVE;

                // notify state changing
                notifyStateChanging(oldState, clientState);
                LOG.info("Finish deactivating maas kafka consumer: {}", clientDefinition);
            }
        });
    }

    private void createNewTenantConsumer(String tenantId, TopicAddress topic) {
        MaasTopicWrap topicWrap = topicMap.get(tenantId);
        if (topicWrap != null) {
            topicWrap.setTopic(topic);
            ConsumerExecContext execContext = createExecContext(topic);

            for (int i = 0; i < instanceCount; i++) {
                consumerWorkerContextMap.computeIfAbsent(tenantId, v -> new ArrayList<>())
                        .add(execContext);
            }

            execContext.getExecutor().init();
            if (clientState.equals(MaasKafkaClientState.ACTIVE)) {
                execContext.getExecutor().start();
            }
        }
    }

    @Override
    public void newActiveTenantEvent(List<String> tenants) {
        if (clientDefinition.isTenant() && !customProcessed) {
            try {
                LOG.info("Start handling tenant-manager event for maas kafka consumer {} with list active tenants {}", clientDefinition, tenants);
                for (String tenantId : tenants) {
                    if (!acceptableTenants.isEmpty() && !acceptableTenants.contains(tenantId)) {
                        continue;
                    }
                    if (!topicMap.containsKey(tenantId)) {
                        topicMap.put(tenantId, new MaasTopicWrap());
                        createNewTenantTopicClient(tenantId, this::createNewTenantConsumer);
                    }
                }

                // remove tenants
                List<String> rmTenants = null;
                for (String existTenant : topicMap.keySet()) {
                    if (!tenants.contains(existTenant)) {
                        try {
                            List<ConsumerExecContext> execContexts = consumerWorkerContextMap.get(existTenant);
                            if (execContexts != null && !execContexts.isEmpty()) {
                                execContexts.forEach(exCtx -> {
                                    try {
                                        exCtx.getExecutor().close();
                                    } catch (Throwable throwable) {
                                        LOG.error("Error occurred during closing consumer for tenant {}", existTenant, throwable);
                                    }
                                });
                            }
                        } catch (Throwable throwable) {
                            LOG.error("Error occurred during closing consumer for tenant {}", existTenant, throwable);
                        }
                        if (rmTenants == null) {
                            rmTenants = new ArrayList<>();
                        }
                        rmTenants.add(existTenant);
                    }
                }
                if (rmTenants != null) {
                    rmTenants.forEach(tenant -> {
                        topicMap.remove(tenant);
                        consumerWorkerContextMap.remove(tenant);
                    });
                    LOG.info("Remove kafka consumers for tenants: {} in maas kafka consumer {}", rmTenants, clientDefinition);
                }
                LOG.info("Finish handling tenant-manager event for maas kafka consumer {} with list active tenants {}", clientDefinition, tenants);
            } catch (Exception ex) {
                LOG.error("Error occurred on adding new tenant", ex);
            }
        }
    }

    @Override
    public MaasKafkaConsumerDefinition getDefinition() {
        return (MaasKafkaConsumerDefinition) clientDefinition;
    }

}
