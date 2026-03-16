package com.netcracker.maas.declarative.kafka.client.impl.client.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.netcracker.cloud.maas.client.api.kafka.TopicAddress;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaClientState;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaClientStateChangeListener;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasKafkaConsumerDefinition;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasTopicDefinition;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.ManagedBy;
import com.netcracker.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import com.netcracker.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

class MaasKafkaCommonClientTest {

    private static final String TEST_TOPIC = "test-topic";
    private static final String TEST_NAMESPACE = "test-namespace";
    private static final String TEST_GROUP = "test-group";
    private static final String TEST_TENANT = "test-tenant";
    private static final int TIMEOUT_SECONDS = 5;

    @Mock
    private InternalTenantService tenantService;
    @Mock
    private MaasKafkaTopicService kafkaTopicService;
    @Mock
    private InternalMaasTopicCredentialsExtractor credentialsExtractor;
    @Mock
    private MaasKafkaClientStateChangeNotificationService stateChangeNotificationService;

    private MaasKafkaCommonClient client;
    private TopicAddress mockTopicAddress;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        List<String> acceptableTenants = new ArrayList<>();
        mockTopicAddress = mock(TopicAddress.class);

        when(kafkaTopicService.getTopicAddressByDefinition(any())).thenReturn(mockTopicAddress);
        when(kafkaTopicService.getOrCreateTopicAddressByDefinition(any())).thenReturn(mockTopicAddress);

        MaasKafkaConsumerDefinition clientDefinition = MaasKafkaConsumerDefinition.builder()
                .setTopic(MaasTopicDefinition.builder()
                        .setName(TEST_TOPIC)
                        .setNamespace(TEST_NAMESPACE)
                        .setManagedBy(ManagedBy.SELF)
                        .build())
                .setGroupId(TEST_GROUP)
                .build();
        client = new TestMaasKafkaCommonClient(
                tenantService,
                kafkaTopicService,
                credentialsExtractor,
                clientDefinition,
                acceptableTenants,
                stateChangeNotificationService
        );
    }

    @Test
    void addAndRemoveChangeStateListener() {
        // Given
        AtomicReference<MaasKafkaClientState> oldState = new AtomicReference<>();
        AtomicReference<MaasKafkaClientState> newState = new AtomicReference<>();
        MaasKafkaClientStateChangeListener listener = createStateChangeListener(oldState, newState);

        // When
        client.addChangeStateListener(listener);
        client.notifyStateChanging(MaasKafkaClientState.NOT_INITIALIZED, MaasKafkaClientState.INITIALIZED);

        // Then
        assertEquals(MaasKafkaClientState.NOT_INITIALIZED, oldState.get());
        assertEquals(MaasKafkaClientState.INITIALIZED, newState.get());
    }

    private MaasKafkaClientStateChangeListener createStateChangeListener(
            AtomicReference<MaasKafkaClientState> oldState,
            AtomicReference<MaasKafkaClientState> newState) {
        return (oldMaasKafkaClientState, newMaasKafkaClientState) -> {
            oldState.set(oldMaasKafkaClientState);
            newState.set(newMaasKafkaClientState);
        };
    }

    @Test
    void getClientState() {
        assertEquals(MaasKafkaClientState.NOT_INITIALIZED, client.getClientState());
    }

    @Test
    void initAsyncSuccess() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Void> future = client.initAsync();
        future.get(TIMEOUT_SECONDS, SECONDS);

        assertEquals(MaasKafkaClientState.INITIALIZED, client.getClientState());
        verify(kafkaTopicService).getTopicAddressByDefinition(any());
    }

    @Test
    void initSyncSuccess() {
        client.initSync();

        assertEquals(MaasKafkaClientState.INITIALIZED, client.getClientState());
        verify(kafkaTopicService).getTopicAddressByDefinition(any());
    }

    @Test
    void activateAsyncSuccess() throws ExecutionException, InterruptedException, TimeoutException {
        client.initSync();
        CompletableFuture<Void> future = client.activateAsync();
        future.get(TIMEOUT_SECONDS, SECONDS);
        verify(stateChangeNotificationService).subscribeOnActivationEvent(any());
    }

    @Test
    void activateSyncSuccess() {
        client.initSync();
        client.activateSync();
        verify(stateChangeNotificationService).subscribeOnActivationEvent(any());
    }

    @Test
    void createNewTenantTopicClient() {
        // Given
        when(kafkaTopicService.getTopicAddressByDefinitionAndTenantId(any(), eq(TEST_TENANT)))
                .thenReturn(mockTopicAddress);
        when(kafkaTopicService.getOrCreateTopicAddressByDefinitionAndTenantId(any(), eq(TEST_TENANT)))
                .thenReturn(mockTopicAddress);

        AtomicReference<String> capturedTenantId = new AtomicReference<>();
        AtomicReference<TopicAddress> capturedTopicAddress = new AtomicReference<>();

        // When
        client.createNewTenantTopicClient(TEST_TENANT, (t, ta) -> {
            capturedTenantId.set(t);
            capturedTopicAddress.set(ta);
        });
        await().atMost(5, SECONDS).until(() -> capturedTenantId.get() != null && capturedTopicAddress.get() != null);

        // Then
        verify(kafkaTopicService).getTopicAddressByDefinitionAndTenantId(any(), eq(TEST_TENANT));
        assertEquals(TEST_TENANT, capturedTenantId.get());
        assertEquals(mockTopicAddress, capturedTopicAddress.get());
    }

    @Test
    void getActiveTenantsInternal_Success() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        List<String> expectedTenants = List.of("tenant1", "tenant2");
        when(tenantService.listAvailableTenants()).thenReturn(expectedTenants);

        // When
        CompletableFuture<List<String>> future = client.getActiveTenantsInternal();
        List<String> result = future.get(TIMEOUT_SECONDS, SECONDS);

        // Then
        assertEquals(expectedTenants, result);
        verify(tenantService).listAvailableTenants();
    }

    @Test
    void getTopicInternal_Success() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        when(kafkaTopicService.getTopicAddressByDefinition(any())).thenReturn(mockTopicAddress);

        // When
        CompletableFuture<TopicAddress> future = client.getTopicInternal(null);
        TopicAddress result = future.get(TIMEOUT_SECONDS, SECONDS);

        // Then
        assertEquals(mockTopicAddress, result);
        verify(kafkaTopicService).getTopicAddressByDefinition(any());
    }

    @Test
    void getTopicInternal_WithTenant_Success() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        when(kafkaTopicService.getTopicAddressByDefinitionAndTenantId(any(), eq(TEST_TENANT)))
                .thenReturn(mockTopicAddress);

        // When
        CompletableFuture<TopicAddress> future = client.getTopicInternal(TEST_TENANT);
        TopicAddress result = future.get(TIMEOUT_SECONDS, SECONDS);

        // Then
        assertEquals(mockTopicAddress, result);
        verify(kafkaTopicService).getTopicAddressByDefinitionAndTenantId(any(), eq(TEST_TENANT));
    }

    @Test
    void getTopicInternal_CreatesTopicWhenManagedBySelf() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        when(kafkaTopicService.getTopicAddressByDefinition(any())).thenReturn(null);
        when(kafkaTopicService.getOrCreateTopicAddressByDefinition(any())).thenReturn(mockTopicAddress);

        // When
        CompletableFuture<TopicAddress> future = client.getTopicInternal(null);
        TopicAddress result = future.get(TIMEOUT_SECONDS, SECONDS);

        // Then
        assertEquals(mockTopicAddress, result);
        verify(kafkaTopicService).getTopicAddressByDefinition(any());
        verify(kafkaTopicService).getOrCreateTopicAddressByDefinition(any());
    }

    @Test
    void createTopic_WithTenant() {
        // Given
        when(kafkaTopicService.getOrCreateTopicAddressByDefinitionAndTenantId(any(), eq(TEST_TENANT)))
                .thenReturn(mockTopicAddress);

        // When
        TopicAddress result = client.createTopic(TEST_TENANT);

        // Then
        assertEquals(mockTopicAddress, result);
        verify(kafkaTopicService).getOrCreateTopicAddressByDefinitionAndTenantId(any(), eq(TEST_TENANT));
    }

    @Test
    void createTopic_WithoutTenant() {
        // Given
        when(kafkaTopicService.getOrCreateTopicAddressByDefinition(any())).thenReturn(mockTopicAddress);

        // When
        TopicAddress result = client.createTopic(null);

        // Then
        assertEquals(mockTopicAddress, result);
        verify(kafkaTopicService).getOrCreateTopicAddressByDefinition(any());
    }

    // Test implementation of abstract class
    private static class TestMaasKafkaCommonClient extends MaasKafkaCommonClient {
        public TestMaasKafkaCommonClient(
                InternalTenantService tenantService,
                MaasKafkaTopicService kafkaTopicService,
                InternalMaasTopicCredentialsExtractor credentialsExtractor,
                MaasKafkaConsumerDefinition clientDefinition,
                List<String> acceptableTenants,
                MaasKafkaClientStateChangeNotificationService stateChangeNotificationService
        ) {
            super(tenantService, kafkaTopicService, credentialsExtractor, clientDefinition, acceptableTenants, stateChangeNotificationService);
        }

        @Override
        protected void activateInitialized() {
            startConsumingActivationEvents();
            clientState = MaasKafkaClientState.ACTIVE;
            notifyStateChanging(MaasKafkaClientState.INITIALIZED, MaasKafkaClientState.ACTIVE);
        }

        @Override
        protected void activateInactive() {
            startConsumingActivationEvents();
            clientState = MaasKafkaClientState.ACTIVE;
            notifyStateChanging(MaasKafkaClientState.INACTIVE, MaasKafkaClientState.ACTIVE);
        }

        @Override
        protected void onDeactivateClientEvent() {
            clientState = MaasKafkaClientState.INACTIVE;
            notifyStateChanging(MaasKafkaClientState.ACTIVE, MaasKafkaClientState.INACTIVE);
        }

        @Override
        public void newActiveTenantEvent(List<String> tenants) {
            // Test implementation
        }

        @Override
        public void close() {
            // Test implementation
        }
    }
}
