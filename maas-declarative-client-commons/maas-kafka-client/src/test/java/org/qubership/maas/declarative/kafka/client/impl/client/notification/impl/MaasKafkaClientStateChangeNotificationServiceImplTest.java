package org.qubership.maas.declarative.kafka.client.impl.client.notification.impl;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class MaasKafkaClientStateChangeNotificationServiceImplTest {

    private final MaasKafkaClientStateChangeNotificationServiceImpl service = new MaasKafkaClientStateChangeNotificationServiceImpl();
    @Test
    void mustNotify() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Boolean> activated = new CompletableFuture<>();
        service.subscribeOnActivationEvent(() -> activated.complete(true));
        service.notifyOnActivationEvent();
        assertTrue(activated.get(1, TimeUnit.MINUTES));

        CompletableFuture<Boolean> deactivated = new CompletableFuture<>();
        service.subscribeOnDeactivationEvent(() -> deactivated.complete(true));
        service.notifyOnDeactivationEvent();
        assertTrue(activated.get(1, TimeUnit.MINUTES));

    }
}