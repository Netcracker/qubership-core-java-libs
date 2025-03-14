package org.qubership.maas.declarative.kafka.client.api;

import org.qubership.cloud.bluegreen.api.model.BlueGreenState;
import org.qubership.cloud.bluegreen.api.model.NamespaceVersion;
import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.model.Version;
import org.qubership.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.impl.util.EnvUtil;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.supports.strategies.DefaultStrategies;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.qubership.cloud.framework.contexts.tenant.context.TenantContext;
import org.qubership.cloud.headerstracking.filters.context.AcceptLanguageContext;
import org.qubership.cloud.maas.bluegreen.kafka.ConsumerConsistencyMode;
import org.qubership.cloud.maas.bluegreen.kafka.OffsetSetupStrategy;
import org.qubership.cloud.maas.client.api.Classifier;
import org.qubership.cloud.maas.client.impl.dto.kafka.v1.TopicInfo;
import org.qubership.cloud.maas.client.impl.kafka.TopicAddressImpl;
import org.qubership.maas.declarative.kafka.client.api.model.MaasKafkaConsumerCreationRequest;
import org.qubership.maas.declarative.kafka.client.api.model.MaasKafkaProducerCreationRequest;
import org.qubership.maas.declarative.kafka.client.api.model.MaasProducerRecord;
import org.qubership.maas.declarative.kafka.client.api.model.definition.*;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter.impl.ContextPropagationFilter;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter.impl.TracingFilter;
import org.qubership.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationService;
import org.qubership.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationServiceImpl;
import org.qubership.maas.declarative.kafka.client.impl.client.factory.MaasKafkaClientFactoryImpl;
import org.qubership.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import org.qubership.maas.declarative.kafka.client.impl.client.producer.MaasKafkaProducerImpl;
import org.qubership.maas.declarative.kafka.client.impl.common.bg.KafkaConsumerConfiguration;
import org.qubership.maas.declarative.kafka.client.impl.common.context.propagation.DefaultContextPropagationServiceImpl;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import org.qubership.maas.declarative.kafka.client.impl.definition.api.MaasKafkaClientDefinitionService;
import org.qubership.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;
import org.qubership.maas.declarative.kafka.client.impl.tracing.TracingService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
class MaasKafkaClientTest {

    @Container
    private static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"));

    public static final String EN_US = "en_US";
    public static final String TEST_TENANT = "test-tenant";
    private static final Supplier<String> RANDOM_STRING = () -> new Random().ints(97, 123)
            .limit(10)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();

    public static final long POLL_DURATION_SEC = 5;
    private static MaasKafkaClientFactory kafkaClientFactory;
    private static MaasKafkaTopicService maasKafkaTopicService;
    private static KafkaClientCreationService kafkaClientCreationService;
    private static InMemoryBlueGreenStatePublisher inMemoryBlueGreenStatePublisher;
    private static KafkaConsumer<String, String> kafkaConsumer;
    private static KafkaProducer<String, String> kafkaProducer;


    @BeforeEach
    void beforeEach() {
        maasKafkaTopicService = mock(MaasKafkaTopicService.class);

        kafkaClientCreationService = spy(new KafkaClientCreationServiceImpl());
        InternalTenantService tenantService = mock(InternalTenantService.class);
        when(tenantService.listAvailableTenants()).thenReturn(List.of(TEST_TENANT));
        TracingService tracingService = mock(TracingService.class);
        inMemoryBlueGreenStatePublisher = new InMemoryBlueGreenStatePublisher("test_namespace");
        kafkaClientFactory = new MaasKafkaClientFactoryImpl(
                tenantService,
                mock(InternalMaasTopicCredentialsExtractor.class),
                maasKafkaTopicService,
                Collections.emptyList(),
                5,
                1000,
                new DefaultContextPropagationServiceImpl(),
                mock(MaasKafkaClientStateChangeNotificationService.class),
                mock(MaasKafkaClientDefinitionService.class),
                kafkaClientCreationService,
                List.of(1000L),
                List.of(
                        new ContextPropagationFilter(new DefaultContextPropagationServiceImpl()),
                        new TracingFilter(tracingService)
                ),
                inMemoryBlueGreenStatePublisher
        );


        kafkaConsumer = new KafkaConsumer<>(consumerProperties());
        kafkaProducer = new KafkaProducer<>(producerProperties());
    }

    @BeforeAll
    static void setupUp() {
        System.setProperty(EnvUtil.NAMESPACE_PROP, "cloud-dev");
        System.setProperty("maas.client.classifier.namespace", "test_namespace");
    }

    @Test
    void producer() throws Exception {
        TopicInfo topicInfo = createTopicInfo();
        when(maasKafkaTopicService.getTopicAddressByDefinition(any())).thenReturn(new TopicAddressImpl(topicInfo));
        try (var maasKafkaProducer = maasKafkaProducer(topicInfo.getName(), false)) {
            String testMsg = "message-" + RANDOM_STRING.get();
            maasKafkaProducer.sendSync(new MaasProducerRecord<>(null, null, testMsg, null, null));

            kafkaConsumer.subscribe(Pattern.compile(topicInfo.getName()));
            ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(POLL_DURATION_SEC));
            assertEquals(1, records.count());
            assertEquals(testMsg, records.iterator().next().value());
        }
    }

    @Test
    void producerWithHiddenSaslConfig() throws Exception {
        TopicInfo topicInfo = createTopicInfo();
        when(maasKafkaTopicService.getTopicAddressByDefinition(any())).thenReturn(new TopicAddressImpl(topicInfo));
        try (var maasKafkaProducer = maasKafkaProducer(topicInfo.getName(), false)) {
            String testMsg = "message-" + RANDOM_STRING.get();
            maasKafkaProducer.sendSync(new MaasProducerRecord<>(null, null, testMsg, null, null));

            kafkaConsumer.subscribe(Pattern.compile(topicInfo.getName()));
            ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(POLL_DURATION_SEC));

            Field field = MaasKafkaProducerImpl.class.getDeclaredField("clientDefinitionWithHiddenConfig");
            field.setAccessible(true);
            assertEquals("[hidden]", ((MaasKafkaCommonClientDefinition) field.get(maasKafkaProducer)).getClientConfig().get("sasl.jaas.config"));
            assertEquals(1, records.count());
            assertEquals(testMsg, records.iterator().next().value());
        }
    }

    @Test
    void producerTenant() throws Exception {
        TopicInfo topicInfo = createTopicInfo();
        TenantContext.set(TEST_TENANT);
        when(maasKafkaTopicService.getTopicAddressByDefinitionAndTenantId(any(), eq(TEST_TENANT))).thenReturn(new TopicAddressImpl(topicInfo));
        String testMsg = "message-" + RANDOM_STRING.get();
        try (var maasKafkaProducer = maasKafkaProducer(topicInfo.getName(), true)) {
            maasKafkaProducer.sendSync(new MaasProducerRecord<>(null, null, testMsg, null, null));

            kafkaConsumer.subscribe(Pattern.compile(topicInfo.getName()));
            ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(POLL_DURATION_SEC));
            assertEquals(1, records.count());
            assertEquals(testMsg, records.iterator().next().value());
        }
    }

    @Test
    void consumer() throws Exception {
        TopicInfo topicInfo = createTopicInfo();
        when(maasKafkaTopicService.getTopicAddressByDefinition(any())).thenReturn(new TopicAddressImpl(topicInfo));
        final CompletableFuture<String> msg = new CompletableFuture<>();
        try (var ignored = maasKafkaConsumer(topicInfo.getName(), record -> msg.complete(record.value()), false, false)) {
            String testMsg = "message-" + RANDOM_STRING.get();
            kafkaProducer.send(new ProducerRecord<>(topicInfo.getName(), testMsg));
            assertEquals(testMsg, msg.get(POLL_DURATION_SEC, TimeUnit.SECONDS));
        }
    }

    @Test
    void consumerTenant() throws Exception {
        TopicInfo topicInfo = createTopicInfo();
        when(maasKafkaTopicService.getTopicAddressByDefinitionAndTenantId(any(), eq(TEST_TENANT))).thenReturn(new TopicAddressImpl(topicInfo));

        String testMsg = "message-" + RANDOM_STRING.get();
        kafkaProducer.send(new ProducerRecord<>(topicInfo.getName(), testMsg));

        final CompletableFuture<String> msg = new CompletableFuture<>();
        try (var ignored = maasKafkaConsumer(topicInfo.getName(), record -> msg.complete(record.value()), true, false)) {
            assertEquals(testMsg, msg.get(POLL_DURATION_SEC, TimeUnit.SECONDS));
        }
    }

    @Test
    void consumerTenantPlaceholder() throws Exception {
        TopicInfo topicInfo = createTopicInfo(new Classifier("test-name", "tenantId", "test-tenant"));
        when(maasKafkaTopicService.getTopicAddressByDefinitionAndTenantId(any(), eq(TEST_TENANT))).thenReturn(new TopicAddressImpl(topicInfo));

        try (var ignored = maasKafkaConsumer(topicInfo.getName(), noop(), true, false, "group-{{tenantId}}-test")) {
            var consumerConfigCaptor = ArgumentCaptor.forClass(KafkaConsumerConfiguration.class);
            verify(kafkaClientCreationService, timeout(10_000).times(1)).createKafkaConsumer(consumerConfigCaptor.capture(), any(), any(), any(), any(), any());
            var consumerConfiguration = consumerConfigCaptor.getValue();
            var configs = consumerConfiguration.getConfigs();
            assertTrue(configs.containsKey("group.id"));
            assertEquals("group-" + TEST_TENANT + "-test", configs.get("group.id"));
        }


        try (var ignored = maasKafkaConsumer(topicInfo.getName(), noop(), true, false, "{{tenantId}}-group-{{tenantId}}-test-{{tenantId}}")) {
            var consumerConfigCaptor = ArgumentCaptor.forClass(KafkaConsumerConfiguration.class);
            verify(kafkaClientCreationService, timeout(10_000).times(2)).createKafkaConsumer(consumerConfigCaptor.capture(), any(), any(), any(), any(), any());
            var consumerConfiguration = consumerConfigCaptor.getValue();
            var configs = consumerConfiguration.getConfigs();
            assertTrue(configs.containsKey("group.id"));
            assertEquals(TEST_TENANT + "-group-" + TEST_TENANT + "-test-" + TEST_TENANT, configs.get("group.id"));
        }

        try (var ignored = maasKafkaConsumer(topicInfo.getName(), noop(), true, false, "group-test")) {
            var consumerConfigCaptor = ArgumentCaptor.forClass(KafkaConsumerConfiguration.class);
            verify(kafkaClientCreationService, timeout(10_000).times(3)).createKafkaConsumer(consumerConfigCaptor.capture(), any(), any(), any(), any(), any());
            var consumerConfiguration = consumerConfigCaptor.getValue();
            var configs = consumerConfiguration.getConfigs();
            assertTrue(configs.containsKey("group.id"));
            assertEquals("group-test", configs.get("group.id"));
        }

        TopicInfo topicInfoNoTenantId = createTopicInfo(new Classifier("test-name"));
        when(maasKafkaTopicService.getTopicAddressByDefinitionAndTenantId(any(), eq(TEST_TENANT))).thenReturn(new TopicAddressImpl(topicInfoNoTenantId));
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> maasKafkaConsumer(topicInfoNoTenantId.getName(), noop(), true, false, "group-test-{{tenantId}}").close()
        );
        assertTrue(ex.getMessage().contains("group 'group-test-{{tenantId}}' contains {{tenantId}}, but topic's classifier has no tenantId"));

    }

    @Test
    void consumerBg() throws Exception {
        TopicInfo topicInfo = createTopicInfo();
        when(maasKafkaTopicService.getTopicAddressByDefinition(any())).thenReturn(new TopicAddressImpl(topicInfo));
        final CompletableFuture<String> msg = new CompletableFuture<>();
        try (var ignored = maasKafkaConsumer(topicInfo.getName(), record -> msg.complete(record.value()), false, true)) {
            String testMsg = "message-" + RANDOM_STRING.get();
            kafkaProducer.send(new ProducerRecord<>(topicInfo.getName(), testMsg));
            assertEquals(testMsg, msg.get(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void consumerBgRecreateOnException() throws Exception {
        TopicInfo topicInfo = createTopicInfo();
        when(maasKafkaTopicService.getTopicAddressByDefinition(any())).thenReturn(new TopicAddressImpl(topicInfo));
        final CompletableFuture<String> firstMsgReceived = new CompletableFuture<>();
        final CompletableFuture<String> secondMsgReceived = new CompletableFuture<>();
        String firstMsg = "message-first-" + RANDOM_STRING.get();
        String secondMsg = "message-second-" + RANDOM_STRING.get();
        AtomicInteger c = new AtomicInteger(0);
        try (var ignored = maasKafkaConsumer(topicInfo.getName(), rec -> {
            int counter = c.getAndIncrement();
            if (counter == 0) {
                throw new RuntimeException();
            } else if (counter == 1) {
                firstMsgReceived.complete(rec.value());
            } else {
                secondMsgReceived.complete(rec.value());
            }
        }, false, true)) {
            kafkaProducer.send(new ProducerRecord<>(topicInfo.getName(), firstMsg));
            assertEquals(firstMsg, firstMsgReceived.get(10, TimeUnit.SECONDS));
            kafkaProducer.send(new ProducerRecord<>(topicInfo.getName(), secondMsg));
            assertEquals(secondMsg, secondMsgReceived.get(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void consumerVersionedTopic() throws Exception {
        TopicInfo topicInfo = createTopicInfo(true);
        when(maasKafkaTopicService.getTopicAddressByDefinition(any())).thenReturn(new TopicAddressImpl(topicInfo));
        final CompletableFuture<List<String>> msg = new CompletableFuture<>();
        List<String> messages = new ArrayList<>(2);
        inMemoryBlueGreenStatePublisher.setBlueGreenState(new BlueGreenState(
                new NamespaceVersion("first", State.ACTIVE, new Version("v1")),
                new NamespaceVersion("second", State.CANDIDATE, new Version("v2")),
                OffsetDateTime.now()
        ));
        try (var ignored = maasKafkaConsumer(topicInfo.getName(), record -> {
            messages.add(record.value());
            if (messages.size() == 2) {
                msg.complete(messages);
            }
        }, false, true)) {
            kafkaProducer.send(new ProducerRecord<>(topicInfo.getName(), null, "", "message-" + RANDOM_STRING.get(), List.of(new RecordHeader("X-Version", "v1".getBytes()))));
            kafkaProducer.send(new ProducerRecord<>(topicInfo.getName(), null, "", "message-" + RANDOM_STRING.get(), List.of(new RecordHeader("X-Version", "v2".getBytes()))));
            assertEquals(2, msg.get(10, TimeUnit.SECONDS).size());
        }
    }

    @Test
    void consumerSingleThreadPerConsumer() throws Exception {
        TopicInfo topicInfo = createTopicInfo();
        when(maasKafkaTopicService.getTopicAddressByDefinition(any())).thenReturn(new TopicAddressImpl(topicInfo));
        final CompletableFuture<String> msg = new CompletableFuture<>();
        try (var ignored = maasKafkaConsumer(topicInfo.getName(), record -> msg.complete(record.value()), false, false, "group-" + RANDOM_STRING.get(), true)) {
            String testMsg = "message-" + RANDOM_STRING.get();
            kafkaProducer.send(new ProducerRecord<>(topicInfo.getName(), testMsg));
            assertEquals(testMsg, msg.get(POLL_DURATION_SEC, TimeUnit.SECONDS));
        }
    }

    @Test
    void contextPropagation() throws Exception {
        TopicInfo topicInfo = createTopicInfo();
        when(maasKafkaTopicService.getTopicAddressByDefinition(any())).thenReturn(new TopicAddressImpl(topicInfo));
        AcceptLanguageContext.set(EN_US);
        TenantContext.set("tenant_id_value_test");
        String expectedTenantId = TenantContext.get();

        final CompletableFuture<String> acceptLanguageContext = new CompletableFuture<>();
        final CompletableFuture<String> acceptLanguageHeader = new CompletableFuture<>();
        final CompletableFuture<String> tenantIdWhenCompleteSync = new CompletableFuture<>();

        try (MaasKafkaProducer maasKafkaProducer = maasKafkaProducer(topicInfo.getName(), false)) {
            String testMsg = "message-" + RANDOM_STRING.get();
            CompletableFuture<RecordMetadata> act = maasKafkaProducer.sendAsync(new MaasProducerRecord<>(null, null, testMsg, null, null));
            act.whenComplete((recordMetadata, throwable) -> {
                tenantIdWhenCompleteSync.complete(TenantContext.get());
            });
        }
        AcceptLanguageContext.clear();

        try (var ignored = maasKafkaConsumer(topicInfo.getName(), record -> {
            acceptLanguageContext.complete(AcceptLanguageContext.get());
            acceptLanguageHeader.complete(new String(record.headers().lastHeader("Accept-Language").value()));
        }, false, false)) {
            assertEquals(EN_US, acceptLanguageContext.get(10, TimeUnit.SECONDS));
            assertEquals(EN_US, acceptLanguageHeader.get(10, TimeUnit.SECONDS));
        }

        String tenantIdWhenCompleteSyncValue = tenantIdWhenCompleteSync.get(10, TimeUnit.SECONDS);
        assertEquals(expectedTenantId, tenantIdWhenCompleteSyncValue);
    }

    MaasKafkaProducer maasKafkaProducer(String topicName, boolean isTenant) {
        // Create topic definition
        MaasTopicDefinition testTopicDefinition = MaasTopicDefinition.builder()
                .setName(topicName)
                .setNamespace("test_namespace")
                .setManagedBy(ManagedBy.SELF)
                .build();
        // Create producer definition
        MaasKafkaProducerDefinition producerDefinition = MaasKafkaProducerDefinition.builder()
                .setTopic(testTopicDefinition)
                .setTenant(isTenant)
                .setClientConfig(Map.ofEntries(
                        Map.entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()),
                        Map.entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()),
                        Map.entry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()),
                        Map.entry("sasl.jaas.config", "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"client\" password=\"client\";")
                ))
                .build();
        // Create producer creation request
        MaasKafkaProducerCreationRequest producerCreationRequest =
                MaasKafkaProducerCreationRequest.builder()
                        .setProducerDefinition(producerDefinition)
                        .setHandler(producerRecord -> producerRecord)
                        .setKeySerializer(new StrTestSerializer())
                        .setValueSerializer(new StrTestSerializer())
                        .build();

        MaasKafkaProducer producer = kafkaClientFactory.createProducer(producerCreationRequest);

        producer.initSync();
        producer.activateSync();
        return producer;
    }

    MaasKafkaConsumer maasKafkaConsumer(String topicName, Consumer<ConsumerRecord<String, String>> handler, boolean isTenant, boolean bluegreen, String groupId) {
        return maasKafkaConsumer(topicName, handler, isTenant, bluegreen, groupId, false);
    }

    MaasKafkaConsumer maasKafkaConsumer(String topicName, Consumer<ConsumerRecord<String, String>> handler, boolean isTenant, boolean bluegreen, String groupId, boolean executorPerConsumer) {
        // Create topic definition
        MaasTopicDefinition testTopicDefinition = MaasTopicDefinition.builder()
                .setName(topicName)
                .setNamespace("test_namespace")
                .setManagedBy(ManagedBy.SELF)
                .build();
        // Create consumer definition
        MaasKafkaConsumerDefinition.Builder consumerBuilder = MaasKafkaConsumerDefinition.builder()
                .setTopic(testTopicDefinition)
                .setTenant(isTenant)
                .setInstanceCount(1)
                .setClientConfig(Map.ofEntries(
                        Map.entry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()),
                        Map.entry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()),
                        Map.entry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()),
                        Map.entry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                        Map.entry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
                ))
                .setGroupId(groupId)
                .setDedicatedThreadPoolSize(5);
        if (bluegreen) {
            consumerBuilder.setBlueGreenDefinition(new MaasKafkaBlueGreenDefinition(
                    true,
                    ConsumerConsistencyMode.GUARANTEE_CONSUMPTION,
                    OffsetSetupStrategy.EARLIEST,
                    true,
                    true
            ));
        }
        // Create consumer creation request
        MaasKafkaConsumerCreationRequest consumerCreationRequest =
                MaasKafkaConsumerCreationRequest.builder()
                        .setConsumerDefinition(consumerBuilder.build())
                        .setHandler(handler)
                        .setKeyDeserializer(new StrTestDeserializer())
                        .setValueDeserializer(new StrTestDeserializer())
                        .build();

        MaasKafkaConsumer consumer = kafkaClientFactory.createConsumer(consumerCreationRequest);

        consumer.initSync();
        consumer.activateSync();
        return consumer;
    }


    MaasKafkaConsumer maasKafkaConsumer(String topicName, Consumer<ConsumerRecord<String, String>> handler, boolean isTenant, boolean bluegreen) {
        return maasKafkaConsumer(topicName, handler, isTenant, bluegreen, "group-" + RANDOM_STRING.get());
    }

    private static class StrTestDeserializer implements Deserializer<String> {
        @Override
        public String deserialize(String topic, byte[] data) {
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    private static class StrTestSerializer implements Serializer<String> {
        @Override
        public byte[] serialize(String topic, String data) {
            if (data == null) {
                return null;
            }
            return data.getBytes(StandardCharsets.UTF_8);
        }
    }

    private static TopicInfo createTopicInfo() {
        return createTopicInfo(false);
    }

    private static TopicInfo createTopicInfo(Classifier classifier) {
        TopicInfo topicInfo = new TopicInfo();
        topicInfo.setName("test-topic-" + RANDOM_STRING.get());
        topicInfo.setAddresses(Map.of("SASL_SSL", List.of(kafka.getBootstrapServers())));
        topicInfo.setClassifier(classifier);
        return topicInfo;
    }

    private static TopicInfo createTopicInfo(boolean versioned) {
        TopicInfo topicInfo = new TopicInfo();
        topicInfo.setName("test-topic-" + RANDOM_STRING.get());
        topicInfo.setAddresses(Map.of("SASL_SSL", List.of(kafka.getBootstrapServers())));
        topicInfo.setClassifier(new Classifier("test"));
        topicInfo.setVersioned(versioned);
        return topicInfo;
    }

    private static <T> Consumer<T> noop() {
        return t -> {
        };
    }

    private Properties consumerProperties() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "group-" + RANDOM_STRING.get());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return properties;
    }

    private Properties producerProperties() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return properties;
    }
}