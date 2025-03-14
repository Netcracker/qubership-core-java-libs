package org.qubership.maas.declarative.kafka.client.impl.client.common;

import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaClient;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaClientState;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaClientStateChangeListener;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import org.qubership.maas.declarative.kafka.client.api.exception.MaasKafkaException;
import org.qubership.maas.declarative.kafka.client.api.exception.MaasKafkaIllegalStateException;
import org.qubership.maas.declarative.kafka.client.api.model.definition.ManagedBy;
import org.qubership.maas.declarative.kafka.client.impl.client.MaasKafkaInternalScheduledSystemExecServiceKeeper;
import org.qubership.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import org.qubership.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import org.qubership.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaCommonClientDefinition;
import org.qubership.maas.declarative.kafka.client.impl.tracing.TracingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;


public abstract class MaasKafkaCommonClient implements MaasKafkaClient {

    private static final Logger LOG = LoggerFactory.getLogger(MaasKafkaCommonClient.class);
    // only for dev purposes
    protected final List<String> acceptableTenants;

    protected volatile MaasKafkaClientState clientState = MaasKafkaClientState.NOT_INITIALIZED;
    // Non tenant
    protected TopicAddress topic;
    // Tenant <TenantId, Topic>
    protected Map<String, MaasTopicWrap> topicMap;

    protected final InternalTenantService tenantService;
    protected final MaasKafkaTopicService kafkaTopicService;
    // Used on activation stage
    protected final InternalMaasTopicCredentialsExtractor credentialsExtractor;

    protected final MaasKafkaCommonClientDefinition clientDefinition;

    // common executor (thread safety approach)
    protected static final ScheduledExecutorService executorService = MaasKafkaInternalScheduledSystemExecServiceKeeper.getExecutorService();

    // notify about state changing
    protected final Map<MaasKafkaClientStateChangeListener, PropertyChangeListener> listenerMap = new ConcurrentHashMap<>();
    protected final PropertyChangeSupport clientStateChangeSupport = new PropertyChangeSupport(this);
    protected static final String CLIENT_STATE = "clientState"; // name for property change support

    private final MaasKafkaClientStateChangeNotificationService maasKafkaClientStateChangeNotificationService;

    public MaasKafkaCommonClient(
            InternalTenantService tenantService,
            MaasKafkaTopicService kafkaTopicService,
            InternalMaasTopicCredentialsExtractor credentialsExtractor,
            MaasKafkaCommonClientDefinition clientDefinition,
            List<String> acceptableTenants,
            MaasKafkaClientStateChangeNotificationService maasKafkaClientStateChangeNotificationService
    ) {
        this.tenantService = tenantService;
        this.kafkaTopicService = kafkaTopicService;
        this.credentialsExtractor = credentialsExtractor;
        this.clientDefinition = clientDefinition;
        this.acceptableTenants = acceptableTenants;
        this.topicMap = new ConcurrentHashMap<>();
        this.maasKafkaClientStateChangeNotificationService = maasKafkaClientStateChangeNotificationService;
    }

    // for test purposes
    protected MaasKafkaCommonClient(
            InternalTenantService tenantService,
            MaasKafkaTopicService kafkaTopicService,
            InternalMaasTopicCredentialsExtractor credentialsExtractor,
            MaasKafkaCommonClientDefinition clientDefinition,
            List<String> acceptableTenants,
            Map<String, MaasTopicWrap> topicMap,
            MaasKafkaClientStateChangeNotificationService maasKafkaClientStateChangeNotificationService,
            TracingService tracingService
    ) {
        this.tenantService = tenantService;
        this.kafkaTopicService = kafkaTopicService;
        this.credentialsExtractor = credentialsExtractor;
        this.clientDefinition = clientDefinition;
        this.acceptableTenants = acceptableTenants;
        this.topicMap = topicMap;
        this.maasKafkaClientStateChangeNotificationService = maasKafkaClientStateChangeNotificationService;
    }


    @Override
    public void addChangeStateListener(MaasKafkaClientStateChangeListener listener) {
        PropertyChangeListener propertyChangeListener = new ClientStatePropertyChangeListener(Objects.requireNonNull(listener));
        listenerMap.put(listener, propertyChangeListener);
        clientStateChangeSupport.addPropertyChangeListener(propertyChangeListener);
    }

    @Override
    public void removeChangeStateListener(MaasKafkaClientStateChangeListener listener) {
        clientStateChangeSupport.removePropertyChangeListener(listenerMap.remove(listener));
    }

    protected void notifyStateChanging(MaasKafkaClientState oldState, MaasKafkaClientState newState) {
        try {
            clientStateChangeSupport.firePropertyChange(CLIENT_STATE, oldState, newState);
        } catch (Throwable throwable) {
            LOG.error("Error occurred during state change notification", throwable);
        }
    }

    @Override
    public MaasKafkaClientState getClientState() {
        return clientState;
    }

    public CompletableFuture<Void> initAsync() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        CompletableFuture
                .runAsync(() -> {
                    try {
                        if (clientState.equals(MaasKafkaClientState.NOT_INITIALIZED)) {
                            clientState = MaasKafkaClientState.INITIALIZING;
                            if (clientDefinition.isTenant()) {
                                getActiveTenantsInternal()
                                        .handle((tenants, ex) -> {
                                            if (ex != null) {
                                                clientState = MaasKafkaClientState.NOT_INITIALIZED;
                                                future.completeExceptionally(ex);
                                            } else {
                                                LOG.debug("Active tenants: {}", tenants);
                                                initializeTenantClient(tenants, future);
                                            }
                                            return null;
                                        });
                            } else {
                                getTopicInternal(null)
                                        .handle((topic, ex) -> {
                                            execute(() -> {
                                                if (ex != null) {
                                                    clientState = MaasKafkaClientState.NOT_INITIALIZED;
                                                    future.completeExceptionally(ex);
                                                } else {
                                                    this.topic = topic;
                                                    clientState = MaasKafkaClientState.INITIALIZED;
                                                    future.complete(null);

                                                    // state change notification
                                                    notifyStateChanging(
                                                            MaasKafkaClientState.NOT_INITIALIZED,
                                                            MaasKafkaClientState.INITIALIZED
                                                    );
                                                }
                                            });
                                            return null;
                                        });
                            }
                        } else {
                            future.completeExceptionally(new MaasKafkaIllegalStateException(getClientState() + " is illegal state for initialization"));
                        }
                    } catch (Throwable throwable) {
                        LOG.error("Initialization failed", throwable);
                        future.completeExceptionally(throwable);
                    }
                }, executorService);

        return future;
    }

    private void initializeTenantClient(List<String> tenantIds, CompletableFuture<Void> future) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String tenantId : tenantIds) {
            if (!acceptableTenants.isEmpty() && !acceptableTenants.contains(tenantId.toString())) {
                continue;
            }
            final String tenant = tenantId;
            CompletableFuture<Void> topicFuture = getTopicInternal(tenantId.toString())
                    .handle((topic, ex) -> {
                        if (ex != null) {
                            throw new RuntimeException(ex);
                        }
                        topicMap.put(tenant.toString(), new MaasTopicWrap(topic));
                        return null;
                    });
            futures.add(topicFuture);
        }
        CompletableFuture[] futureArray = new CompletableFuture[futures.size()];
        CompletableFuture.allOf(futures.toArray(futureArray))
                .handle((v, ex) -> {
                    execute(() -> {
                        if (ex != null) {
                            clientState = MaasKafkaClientState.NOT_INITIALIZED;
                            future.completeExceptionally(ex);
                        } else {
                            clientState = MaasKafkaClientState.INITIALIZED;
                            future.complete(null);

                            // state change notification
                            notifyStateChanging(
                                    MaasKafkaClientState.NOT_INITIALIZED,
                                    MaasKafkaClientState.INITIALIZED
                            );
                        }
                    });
                    return null;
                });
    }

    public void initSync() {
        try {
            initAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new MaasKafkaException(e.getMessage(), e);
        }
    }

    // Getting tenants methods
    protected CompletableFuture<List<String>> getActiveTenantsInternal() {
        final CompletableFuture<List<String>> result = new CompletableFuture<>();
        execute(() -> {
            try {
                List<String> tenants = _getAvailableTenants();
                if (tenants == null) {
                    LOG.warn("Available tenants haven't been fetched, start waiting process");
                    execute(() -> resolveAvailableTenantsInternal(result));
                } else {
                    result.complete(tenants);
                }
            } catch (Throwable th) {
                result.completeExceptionally(th);
            }
        });
        return result;
    }

    private void resolveAvailableTenantsInternal(CompletableFuture<List<String>> result) {
        List<String> tenants = _getAvailableTenants();
        if (tenants == null) {
            LOG.debug("Available tenants haven't been fetched");
            executorService.schedule(() -> {
                resolveAvailableTenantsInternal(result);
            }, MaasKafkaCommonConstants.THREAD_WAIT, TimeUnit.MILLISECONDS);
        } else {
            result.complete(tenants);
        }
    }

    private List<String> _getAvailableTenants() {
        List<String> tenants;
        try {
            tenants = tenantService.listAvailableTenants();
        } catch (Exception ex) {
            LOG.error("Error occurred during getting list available tenants", ex);
            return null;
        }
        return tenants;
    }


    // Getting topic methods
    protected CompletableFuture<TopicAddress> getTopicInternal(String tenantId) {
        LOG.debug("Try get topic for tenantId: {}", tenantId);
        CompletableFuture<TopicAddress> result = new CompletableFuture<>();
        TopicAddress topic = _getTopic(tenantId);
        if (topic == null) {
            LOG.warn("There is no necessary topic {}, start waiting process", clientDefinition.getTopic());
            executorService.submit(() -> resolveTopicInternal(tenantId, result));
        } else {
            result.complete(topic);
        }
        return result;
    }

    private TopicAddress _getTopic(String tenantId) {
        TopicAddress topic;
        try {
            if (tenantId != null) {
                topic = kafkaTopicService.getTopicAddressByDefinitionAndTenantId(clientDefinition, tenantId);
            } else {
                topic = kafkaTopicService.getTopicAddressByDefinition(clientDefinition);
            }

            if (topic == null && ManagedBy.SELF.equals(clientDefinition.getTopic().getManagedBy())) {
                topic = createTopic(tenantId);
            }
        } catch (Exception ex) {
            LOG.error("Error during topic initialization for tenantId: {}, topicDefinition: {}", tenantId, clientDefinition.getTopic(), ex);
            return null;
        }
        return topic;
    }

    private void resolveTopicInternal(String tenantId, CompletableFuture<TopicAddress> result) {
        TopicAddress topic = _getTopic(tenantId);
        if (topic == null) {
            LOG.debug("Submit retry initialization process for tenantId: {}, topicDefinition: {}", tenantId, clientDefinition.getTopic());
            executorService.schedule(() -> {
                resolveTopicInternal(tenantId, result);
            }, MaasKafkaCommonConstants.THREAD_WAIT, TimeUnit.MILLISECONDS);
        } else {
            result.complete(topic);
        }
    }


    private TopicAddress createTopic(String tenantId) {
        TopicAddress topic;
        if (tenantId != null) {
            topic = kafkaTopicService.getOrCreateTopicAddressByDefinitionAndTenantId(clientDefinition, tenantId);
        } else {
            topic = kafkaTopicService.getOrCreateTopicAddressByDefinition(clientDefinition);
        }
        return topic;
    }

    public CompletableFuture<Void> activateAsync() {
        return CompletableFuture
                .runAsync(() -> {
                    try {
                        if (MaasKafkaClientState.INITIALIZED.equals(clientState)) {
                            activateInitialized();
                        } else if (MaasKafkaClientState.INACTIVE.equals(clientState)) {
                            activateInactive();
                        } else {
                            throw new MaasKafkaIllegalStateException(getClientState() + " is illegal state for activation");
                        }
                    } catch (Throwable throwable) {
                        LOG.error("Activation failed", throwable);
                        throw throwable;
                    }
                }, executorService);
    }

    public void activateSync() {
        try {
            activateAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    // for creating new consumer/producer
    protected void createNewTenantTopicClient(String tenantId, BiConsumer<String, TopicAddress> creator) {
        getTopicInternal(tenantId)
                .handle((topic, ex) -> {
                    if (ex != null) {
                        LOG.error("Error occurred during getting topic for tenant {}", tenantId, ex);
                    } else {
                        execute(() -> {
                            creator.accept(tenantId, topic);
                        });
                    }
                    return null;
                });
    }

    protected abstract void activateInitialized();

    protected abstract void activateInactive();

    protected void startConsumingActivationEvents() {
        maasKafkaClientStateChangeNotificationService.subscribeOnActivationEvent(this::onActivateClientEvent);
    }

    protected void startConsumingDeactivationEvents() {
        maasKafkaClientStateChangeNotificationService.subscribeOnDeactivationEvent(this::onDeactivateClientEvent);
    }

    protected void onActivateClientEvent() {
        activateAsync();
    }

    protected abstract void onDeactivateClientEvent();

    public abstract void newActiveTenantEvent(List<String> tenants);

    // Used to prevent concurrency state changing operations
    protected Future<?> execute(Runnable runner) {
        return executorService.submit(runner);
    }

}
