package org.qubership.maas.declarative.kafka.client.impl.client.state.manager;

import org.qubership.maas.declarative.kafka.client.api.MaasKafkaClient;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaClientState;
import org.qubership.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class MaasKafkaClientStateManagerImplTest {

    @Test
    void shouldInitialize() throws ExecutionException, InterruptedException, TimeoutException {
        MaasKafkaClientStateChangeNotificationService notificationService = mock(MaasKafkaClientStateChangeNotificationService.class);
        MaasKafkaClient maasKafkaClient = mock(MaasKafkaClient.class);
        when(maasKafkaClient.getClientState()).thenReturn(MaasKafkaClientState.INITIALIZED);
        List<MaasKafkaClient> maasKafkaClients = List.of(maasKafkaClient);

        MaasKafkaClientStateManagerImpl maasKafkaClientStateManager = new MaasKafkaClientStateManagerImpl(notificationService, maasKafkaClients);
        CompletableFuture<Boolean> called = new CompletableFuture<>();
        maasKafkaClientStateManager.activateInitializedClients((maasKafkaClient1, voidCompletableFuture) -> called.complete(true));
        assertTrue(called.get(1, TimeUnit.SECONDS));
        when(maasKafkaClient.getClientState()).thenReturn(MaasKafkaClientState.INITIALIZED);
        maasKafkaClientStateManager.activateInitializedClients(null);
        verify(maasKafkaClient, timeout(10_000).times(2)).activateAsync();
    }
}