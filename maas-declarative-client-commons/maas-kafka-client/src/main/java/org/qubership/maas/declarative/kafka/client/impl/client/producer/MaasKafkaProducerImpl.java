package org.qubership.maas.declarative.kafka.client.impl.client.producer;

import org.qubership.cloud.framework.contexts.tenant.context.TenantContext;
import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaClientState;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaProducer;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import org.qubership.maas.declarative.kafka.client.api.context.propagation.ContextPropagationService;
import org.qubership.maas.declarative.kafka.client.api.exception.MaasKafkaException;
import org.qubership.maas.declarative.kafka.client.api.exception.MaasKafkaNoResourceException;
import org.qubership.maas.declarative.kafka.client.api.model.MaasKafkaProducerCreationRequest;
import org.qubership.maas.declarative.kafka.client.api.model.MaasProducerRecord;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaCommonClientDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition;
import org.qubership.maas.declarative.kafka.client.impl.Utils;
import org.qubership.maas.declarative.kafka.client.impl.client.common.MaasKafkaCommonClient;
import org.qubership.maas.declarative.kafka.client.impl.client.common.MaasTopicWrap;
import org.qubership.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationService;
import org.qubership.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import org.qubership.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;
import org.qubership.maas.declarative.kafka.client.impl.tracing.TracingService;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.qubership.maas.declarative.kafka.client.impl.Utils.safe;

public class MaasKafkaProducerImpl extends MaasKafkaCommonClient implements MaasKafkaProducer {
    private static final Logger LOG = LoggerFactory.getLogger(MaasKafkaProducerImpl.class);

    private static final String SASL_CONFIG = "sasl.jaas.config";
    private static final String HIDDEN_STR = "[hidden]";

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

    public MaasKafkaProducerImpl(
            MaasKafkaProducerCreationRequest creationRequest,
            InternalTenantService tenantService,
            MaasKafkaTopicService kafkaTopicService,
            InternalMaasTopicCredentialsExtractor credentialsExtractor,
            List<String> acceptableTenants,
            ContextPropagationService contextPropagationService,
            MaasKafkaClientStateChangeNotificationService maasKafkaClientStateChangeNotificationService,
            KafkaClientCreationService kafkaClientCreationService
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
    }

    // for test purposes

    private final KafkaClientCreationService kafkaClientCreationService;

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
            try {
                LOG.info("Start deactivating maas kafka producer clients");
                if (clientDefinition.isTenant()) {
                    producerMap.forEach((key, value) -> value.close());
                } else {
                    producer.close();
                }
                clientState = MaasKafkaClientState.INACTIVE;

                // notify state changing
                notifyStateChanging(
                        MaasKafkaClientState.ACTIVE,
                        MaasKafkaClientState.INACTIVE
                );
                LOG.info("Finish deactivating maas kafka producer clients");
            } catch (Exception ex) {
                LOG.error("Deactivation error", ex);
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
        try {
            TopicAddress prodTopic = this.getTopic();
            Producer prod;

            LOG.debug("Sending record {}", record);

            if (prodTopic != null) {
                Headers headers = record.getHeaders() != null
                        ? record.getHeaders()
                        : new RecordHeaders();


                ProducerRecord producerRecord = handler.apply(new ProducerRecord(
                        prodTopic.getTopicName(),
                        record.getPartition(),
                        record.getTimestamp(),
                        record.getKey(),
                        record.getValue(),
                        headers
                ));

                contextPropagationService.populateDataToHeaders(producerRecord);

                if (clientDefinition.isTenant()) {
                    prod = producerMap.computeIfAbsent(TenantContext.get(), (key) -> {
                        // to prevent NPE
                        throw new MaasKafkaException("No producer for Tenant " + key);
                    });
                } else {
                    prod = producer;
                }

                prod.send(producerRecord, new ContextAwareCallback((md, ex) -> {
                    if (ex != null) {
                        future.completeExceptionally(ex);
                    } else {
                        future.complete(md);
                    }
                }
                ));
            } else {
                throw new MaasKafkaNoResourceException("No valid topic");
            }
        } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }

        return future;
    }

    @Override
    public <K, V> RecordMetadata sendSync(MaasProducerRecord<K, V> record) {
        try {
            return sendAsync(record).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
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

    private Producer createKafkaProducer(Map<String, Object> config) {
        return kafkaClientCreationService.createKafkaProducer(config, keySerializer, valueSerializer);
    }
}
