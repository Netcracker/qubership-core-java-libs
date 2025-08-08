package com.netcracker.maas.declarative.kafka.client.api;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Event emitter for change clients state in runtime
 */
public interface MaasKafkaClientStateManagerService {

    /**
     * Emits activation event to all maas kafka clients
     * Note this method can be used after client activation otherwise this method can't change client state
     */
    void emitClientActivationEvent();

    /**
     * Emits deactivation event to all maas kafka clients
     * Note this method can be used after client activation otherwise this method can't change client state
     */
    void emitClientDeactivationEvent();

    /**
     * This method asynchronous start infinite activation process until all client moves their states from {@link MaasKafkaClientState#INITIALIZED}
     * to {@link MaasKafkaClientState#ACTIVE}, if some of clients became active externally (i.e. will be
     * in one of two states {@link MaasKafkaClientState#ACTIVE} or {@link MaasKafkaClientState#INACTIVE}), then it will be removed
     * from activation queue (because the method guess that this client already has been properly activated)
     * @param afterActivationCallback - callback function that will be called after activation for each client
     *                                (if this param will be null, thus default callback will be called)
     */
    void activateInitializedClients(BiConsumer<MaasKafkaClient, CompletableFuture<Void>> afterActivationCallback);
}
