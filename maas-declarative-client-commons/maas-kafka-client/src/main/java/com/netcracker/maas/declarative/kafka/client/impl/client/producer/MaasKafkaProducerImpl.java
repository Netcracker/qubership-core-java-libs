package com.netcracker.maas.declarative.kafka.client.impl.client.producer;

import com.netcracker.cloud.framework.contexts.tenant.context.TenantContext;
import com.netcracker.cloud.maas.client.api.kafka.TopicAddress;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaClientState;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaProducer;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import com.netcracker.maas.declarative.kafka.client.api.context.propagation.ContextPropagationService;
import com.netcracker.maas.declarative.kafka.client.api.exception.MaasKafkaException;
import com.netcracker.maas.declarative.kafka.client.api.exception.MaasKafkaNoResourceException;
import com.netcracker.maas.declarative.kafka.client.api.model.MaasKafkaProducerCreationRequest;
import com.netcracker.maas.declarative.kafka.client.api.model.MaasProducerRecord;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasKafkaCommonClientDefinition;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition;
import com.netcracker.maas.declarative.kafka.client.impl.Utils;
import com.netcracker.maas.declarative.kafka.client.impl.client.common.MaasKafkaCommonClient;
import com.netcracker.maas.declarative.kafka.client.impl.client.common.MaasTopicWrap;
import com.netcracker.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationService;
import com.netcracker.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import com.netcracker.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;
import com.netcracker.maas.declarative.kafka.client.impl.tracing.TracingService;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static com.netcracker.maas.declarative.kafka.client.impl.Utils.safe;

public class MaasKafkaProducerImpl extends MaasKafkaCommonClient implements MaasKafkaProducer {
    private static final Logger LOG = LoggerFactory.getLogger(MaasKafkaProducerImpl.class);

    private static final String SASL_CONFIG = "sasl.jaas.config";
    private static final String HIDDEN_STR = "[hidden]";

    /** Kafka default for {@link ProducerConfig#DELIVERY_TIMEOUT_MS_CONFIG}. */
    static final long DEFAULT_DELIVERY_TIMEOUT_MS = 120_000L;

    /** Extra slack on top of delivery.timeout.ms when deriving sendSync wait. */
    static final long SEND_SYNC_TIMEOUT_SLACK_MS = 5_000L;

    /** Floor so a misconfigured timeout cannot be zero or negative. */
    static final long MIN_SEND_SYNC_TIMEOUT_MS = 1_000L;

    private static final long SLOW_SEND_WARN_MS = 5_000L;

    // Non tenant
    private Producer producer;
    // Tenant
    private final Map<String, Producer> producerMap = new ConcurrentHashMap<>();
    // Serializers
    private Serializer keySerializer;
    private Serializer valueSerializer;

    private MaasKafkaCommonClientDefinition clientDefinitionWithHiddenConfig;
    // Producer record handler
    private Function<ProducerRecord, ProducerRecord> handler = Function.identity();

    private final ContextPropagationService contextPropagationService;
    private final AtomicBoolean sendSyncTimeoutLogged = new AtomicBoolean(false);
    /** Global {@code maas.kafka.send-sync-timeout-ms}; definition override wins when set. */
    private final Long globalSendSyncTimeoutMs;

    // for test purposes
    private final KafkaClientCreationService kafkaClientCreationService;

    public MaasKafkaProducerImpl(
            MaasKafkaProducerCreationRequest creationRequest,
            InternalTenantService tenantService,
            MaasKafkaTopicService kafkaTopicService,
            InternalMaasTopicCredentialsExtractor credentialsExtractor,
            List<String> acceptableTenants,
            ContextPropagationService contextPropagationService,
            MaasKafkaClientStateChangeNotificationService maasKafkaClientStateChangeNotificationService,
            KafkaClientCreationService kafkaClientCreationService,
            Long globalSendSyncTimeoutMs
    ) {
        super(
                tenantService,
                kafkaTopicService,
                credentialsExtractor,
                creationRequest.getProducerDefinition(),
                acceptableTenants,
                maasKafkaClientStateChangeNotificationService
        );

        this.keySerializer = creationRequest.getKeySerializer();
        this.valueSerializer = creationRequest.getValueSerializer();
        this.handler = creationRequest.getHandler();
        this.contextPropagationService = contextPropagationService;
        this.kafkaClientCreationService = kafkaClientCreationService;
        this.globalSendSyncTimeoutMs = globalSendSyncTimeoutMs;
    }

    protected MaasKafkaProducerImpl(
            InternalTenantService tenantService,
            MaasKafkaTopicService kafkaTopicService,
            InternalMaasTopicCredentialsExtractor credentialsExtractor,
            MaasKafkaCommonClientDefinition clientDefinition,
            List<String> acceptableTenants,
            Map<String, MaasTopicWrap> topicMap,
            ContextPropagationService contextPropagationService,
            MaasKafkaClientStateChangeNotificationService maasKafkaClientStateChangeNotificationService,
            KafkaClientCreationService kafkaClientCreationService,
            TracingService tracingService
    ) {
        super(
                tenantService,
                kafkaTopicService,
                credentialsExtractor,
                clientDefinition,
                acceptableTenants,
                topicMap,
                maasKafkaClientStateChangeNotificationService,
                tracingService
        );
        this.contextPropagationService = contextPropagationService;
        this.kafkaClientCreationService = kafkaClientCreationService;
        this.globalSendSyncTimeoutMs = null;
    }

    @Override
    public void close() {
        try {
            if (clientDefinition.isTenant()) {
                producerMap.values().forEach(p -> safe(() -> p.close()));
            } else {
                if (producer != null) {
                    producer.close();
                }
            }
        } catch (Exception e) {
            LOG.error("Error destroy consumer", e);
        }
    }

    @Override
    protected void activateInitialized() {
        commonActivation();

        tenantService.subscribe(tenants -> {
            execute(() -> newActiveTenantEvent(tenants));
        });
        // start event consuming
        startConsumingActivationEvents();
        startConsumingDeactivationEvents();
    }

    @Override
    protected void activateInactive() {
        commonActivation();
    }

    private void commonActivation() {
        LOG.info("Start activation maas kafka producer: {}", clientDefinitionWithHiddenConfigValue(clientDefinition));
        if (clientDefinition.isTenant()) {
            topicMap.forEach((key, value) -> {
                Map<String, Object> connectionProps = credentialsExtractor.extract(value.getTopic());
                producerMap.put(
                        key,
                        createKafkaProducer(Utils.merge(clientDefinition.getClientConfig(), connectionProps))
                );
            });
        } else {
            Map<String, Object> connectionProps = credentialsExtractor.extract(topic);
            producer = createKafkaProducer(Utils.merge(clientDefinition.getClientConfig(), connectionProps));
        }

        MaasKafkaClientState oldState = clientState;
        clientState = MaasKafkaClientState.ACTIVE;

        // notify state changing
        notifyStateChanging(
                oldState,
                clientState
        );
        LOG.info("Finish activation maas kafka producer: {}", clientDefinitionWithHiddenConfigValue(clientDefinition));
    }

    private MaasKafkaCommonClientDefinition clientDefinitionWithHiddenConfigValue(MaasKafkaCommonClientDefinition clientDefinition) {
        HashMap<String, Object> clientConfigObj = new HashMap<>(clientDefinition.getClientConfig());
        if (!clientConfigObj.isEmpty() && clientConfigObj.containsKey(SASL_CONFIG)) {
            clientConfigObj.put(SASL_CONFIG, HIDDEN_STR);
        }
        clientDefinitionWithHiddenConfig = new MaasKafkaCommonClientDefinition(clientDefinition.getTopic(), clientDefinition.isTenant(), clientConfigObj) {
        };
        return clientDefinitionWithHiddenConfig;
    }

    @Override
    protected void onDeactivateClientEvent() {
        execute(() -> {
            LOG.info("Start deactivating maas kafka producer clients");
            MaasKafkaClientState oldState = clientState;
            try {
                if (clientDefinition.isTenant()) {
                    producerMap.values().forEach(p -> safe(() -> p.close()));
                } else {
                    safe(() -> producer.close());
                }
            } catch (Exception ex) {
                // closing is best effort, the state is not: a client left reporting ACTIVE
                // cannot be revived, because activateAsync refuses that state
                LOG.error("Deactivation error", ex);
            } finally {
                clientState = MaasKafkaClientState.INACTIVE;
                notifyStateChanging(oldState, MaasKafkaClientState.INACTIVE);
                LOG.info("Finish deactivating maas kafka producer clients");
            }
        });
    }

    private void createNewTenantProducer(String tenantId, TopicAddress topic) {
        MaasTopicWrap topicWrap = topicMap.get(tenantId);
        if (topicWrap != null) {
            topicWrap.setTopic(topic);
            Map<String, Object> connectionProps = credentialsExtractor.extract(topic);
            producerMap.put(
                    tenantId,
                    createKafkaProducer(Utils.merge(clientDefinition.getClientConfig(), connectionProps))
            );
            if (clientState.equals(MaasKafkaClientState.INACTIVE)) {
                try {
                    producerMap.get(tenantId).close();
                } catch (Throwable throwable) {
                    LOG.error("Error occurred during closing producer for topic {} and tenant {}", topic, tenantId, throwable);
                }
            }
        }
    }

    @Override
    public void newActiveTenantEvent(List<String> tenants) {
        if (clientDefinition.isTenant()) {
            try {
                for (String tenantId : tenants) {
                    if (!acceptableTenants.isEmpty() && !acceptableTenants.contains(tenantId)) {
                        continue;
                    }
                    if (!topicMap.containsKey(tenantId)) {
                        topicMap.put(tenantId, new MaasTopicWrap());
                        createNewTenantTopicClient(tenantId, this::createNewTenantProducer);
                    }
                }

                // remove tenant
                List<String> rmTenants = null;
                for (String existTenant : topicMap.keySet()) {
                    if (!tenants.contains(existTenant)) {
                        try {
                            Producer producer = producerMap.get(existTenant);
                            if (producer != null) {
                                producer.close();
                            }
                        } catch (Exception ex) {
                            LOG.error("Error occurred during closing producer for tenant {}", existTenant, ex);
                        }
                        if (rmTenants == null) {
                            rmTenants = new ArrayList<>();
                        }
                        rmTenants.add(existTenant);
                    }
                }
                if (rmTenants != null) {
                    rmTenants.forEach(tenant -> {
                        try {
                            topicMap.remove(tenant);
                            producerMap.remove(tenant);
                        } catch (Throwable throwable) {
                            LOG.error(throwable.getMessage(), throwable);
                        }
                    });
                }
            } catch (Exception ex) {
                LOG.error("Error occurred on handling tenant manager event", ex);
            }
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
    public <K, V> CompletableFuture<RecordMetadata> sendAsync(MaasProducerRecord<K, V> record) {
        CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
        String topicName = null;
        String tenant = null;
        try {
            TopicAddress topic = resolveTopic();
            topicName = topic.getTopicName();
            tenant = clientDefinition.isTenant() ? TenantContext.get() : null;

            Producer<K, V> prod = resolveProducer(tenant);
            ProducerRecord<K, V> producerRecord = buildRecord(topic, record);
            doSend(prod, producerRecord, future);
        } catch (Exception e) {
            LOG.error("Failed to submit record to Kafka: topic={}, tenant={}, key={}",
                    topicName, tenant, record.getKey(), e);
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public <K, V> RecordMetadata sendSync(MaasProducerRecord<K, V> record) {
        long timeoutMs = resolveSendSyncTimeoutMs();
        return awaitDelivery(sendAsync(record), record, timeoutMs);
    }

    @Override
    public <K, V> RecordMetadata sendSync(MaasProducerRecord<K, V> record, Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        long timeoutMs = timeout.toMillis();
        if (timeoutMs < 1) {
            throw new IllegalArgumentException("timeout must be at least 1 ms");
        }
        return awaitDelivery(sendAsync(record), record, timeoutMs);
    }

    @Override
    public Producer unwrap() {
        return producer;
    }

    @Override
    public Producer unwrap(String tenantId) {
        return producerMap.get(TenantContext.get());
    }

    @Override
    public MaasKafkaProducerDefinition getDefinition() {
        return (MaasKafkaProducerDefinition) clientDefinition;
    }

    private TopicAddress resolveTopic() {
        TopicAddress resolved = this.getTopic();
        if (resolved == null) {
            throw new MaasKafkaNoResourceException("No valid topic");
        }
        return resolved;
    }

    @SuppressWarnings("unchecked")
    private <K, V> Producer<K, V> resolveProducer(String tenant) {
        if (!clientDefinition.isTenant()) {
            return (Producer<K, V>) producer;
        }
        if (tenant == null || tenant.isEmpty()) {
            throw new MaasKafkaException("Tenant context is empty for tenant-aware producer");
        }
        Producer<K, V> prod = (Producer<K, V>) producerMap.get(tenant);
        if (prod == null) {
            throw new MaasKafkaException("No producer for tenant " + tenant
                    + ", known tenants: " + producerMap.keySet());
        }
        return prod;
    }

    @SuppressWarnings("unchecked")
    private <K, V> ProducerRecord<K, V> buildRecord(TopicAddress topic, MaasProducerRecord<K, V> record) {
        Headers headers = record.getHeaders() != null ? record.getHeaders() : new RecordHeaders();
        ProducerRecord<K, V> producerRecord = (ProducerRecord<K, V>) handler.apply(new ProducerRecord<>(
                topic.getTopicName(),
                record.getPartition(),
                record.getTimestamp(),
                record.getKey(),
                record.getValue(),
                headers));
        contextPropagationService.populateDataToHeaders(producerRecord);
        LOG.debug("Sending record to topic={}, partition={}, key={}",
                producerRecord.topic(), producerRecord.partition(), producerRecord.key());
        return producerRecord;
    }

    private <K, V> void doSend(Producer<K, V> prod, ProducerRecord<K, V> producerRecord,
                               CompletableFuture<RecordMetadata> future) {
        long startNanos = System.nanoTime();
        Callback contextAware = new ContextAwareCallback((md, ex) -> {
            long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            if (ex != null) {
                future.completeExceptionally(ex);
            } else {
                if (tookMs > SLOW_SEND_WARN_MS) {
                    LOG.warn("Slow Kafka delivery: topic={}, partition={}, offset={}, tookMs={}",
                            md.topic(), md.partition(), md.offset(), tookMs);
                }
                future.complete(md);
            }
        });

        // Future must complete even if ContextAwareCallback fails before calling the delegate
        prod.send(producerRecord, (md, ex) -> {
            try {
                contextAware.onCompletion(md, ex);
            } catch (Throwable t) {
                LOG.error("Kafka callback failed: topic={}, key={}", producerRecord.topic(), producerRecord.key(), t);
                future.completeExceptionally(t);
            } finally {
                if (!future.isDone()) {
                    future.completeExceptionally(new IllegalStateException(
                            "Kafka callback finished without completing future", ex));
                }
            }
        });
    }

    private RecordMetadata awaitDelivery(CompletableFuture<RecordMetadata> future,
                                         MaasProducerRecord<?, ?> record,
                                         long timeoutMs) {
        long startNanos = System.nanoTime();
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while waiting for Kafka ack, key={}", record.getKey());
            throw new MaasKafkaException("Interrupted while waiting for Kafka ack", e);
        } catch (TimeoutException e) {
            long waitedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            Long deliveryTimeoutMs = readOptionalLongClientConfig(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG);
            LOG.error("No Kafka ack within {} ms (configuredTimeoutMs={}, delivery.timeout.ms={}), key={}",
                    waitedMs, timeoutMs, deliveryTimeoutMs, record.getKey());
            throw new MaasKafkaException("No Kafka ack within " + waitedMs + " ms", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            LOG.warn("Kafka delivery failed: key={}, cause={}", record.getKey(), cause.toString());
            throw new MaasKafkaException("Kafka send failed: " + cause.getMessage(), cause);
        }
    }

    /**
     * Resolve wall-clock bound for {@link #sendSync(MaasProducerRecord)} before {@code Producer.send}.
     * Order: {@link MaasKafkaProducerDefinition#getSendSyncTimeoutMs()}, else the global property,
     * else {@code delivery.timeout.ms + slack}. An override shorter than {@code delivery.timeout.ms + slack}
     * is rejected. The derived value is floored at 1s.
     */
    long resolveSendSyncTimeoutMs() {
        Long override = getDefinition().getSendSyncTimeoutMs() != null
                ? getDefinition().getSendSyncTimeoutMs()
                : globalSendSyncTimeoutMs;
        long deliveryTimeoutMs = readLongClientConfigOrDefault(
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, DEFAULT_DELIVERY_TIMEOUT_MS);
        long minimum = deliveryTimeoutMs + SEND_SYNC_TIMEOUT_SLACK_MS;
        long resolved;
        if (override != null) {
            if (override < minimum) {
                throw new MaasKafkaException("sendSync timeout " + override
                        + " ms is below delivery.timeout.ms + " + SEND_SYNC_TIMEOUT_SLACK_MS
                        + " ms (" + minimum + ")");
            }
            resolved = override;
        } else {
            resolved = minimum;
        }
        resolved = Math.max(resolved, MIN_SEND_SYNC_TIMEOUT_MS);
        if (sendSyncTimeoutLogged.compareAndSet(false, true)) {
            LOG.info("Resolved sendSync timeout: configuredTimeoutMs={}, delivery.timeout.ms={}, override={}",
                    resolved, deliveryTimeoutMs, override);
        }
        return resolved;
    }

    private long readLongClientConfigOrDefault(String key, long defaultValue) {
        Long value = readOptionalLongClientConfig(key);
        return value != null ? value : defaultValue;
    }

    private Long readOptionalLongClientConfig(String key) {
        Map<String, Object> config = clientDefinition.getClientConfig();
        if (config == null || !config.containsKey(key) || config.get(key) == null) {
            return null;
        }
        Object value = config.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str) {
            try {
                return Long.parseLong(str.trim());
            } catch (NumberFormatException e) {
                throw new MaasKafkaException("Invalid numeric config for " + key + ": " + value, e);
            }
        }
        throw new MaasKafkaException("Unsupported config type for " + key + ": " + value.getClass().getName());
    }

    private Producer createKafkaProducer(Map<String, Object> config) {
        return kafkaClientCreationService.createKafkaProducer(config, keySerializer, valueSerializer);
    }
}
