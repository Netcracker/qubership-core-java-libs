package org.qubership.maas.declarative.kafka.client.api;

import java.util.concurrent.CompletableFuture;

/**
 * Main interface for all kafka clients (producer/consumer)
 */
public interface MaasKafkaClient extends AutoCloseable{

    /**
     * @return current client state
     */
    MaasKafkaClientState getClientState();

    /**
     * Asynchronously initialize client (get or create topics from config using kafka or maas).
     * This method starts initializing process in separate thread to get necessary data from maas or kafka, if no data will be obtained,
     * then the retrying process starts until data will be available (maas or kafka).
     * Note if any external system necessary for getting initializing data won't be available, this method continues retrying
     * until success. After successful execution as result null value will be returned.
     *
     * @throws IllegalStateException - if current client state is not {@link MaasKafkaClientState#NOT_INITIALIZED}
     * @throws RuntimeException      - if any system error occurred (or will be passed to handler as exception param)
     * @throws Error                 - if any system error occurred (or will be passed to handler as exception param)
     * @return completable future instance
     */
    CompletableFuture<Void> initAsync();

    /**
     * Synchronously initialize client (get or create topics from config using kafka or maas).
     * This method starts initializing process to get necessary data from maas or kafka, if no data will be obtained,
     * then the retrying process starts until data will be available (maas or kafka).
     * Note if any external system necessary for getting initializing data won't be available, this method continues retrying
     * until success.
     * Note this method can block thread for long time
     *
     * @throws IllegalStateException - if current client state is not {@link MaasKafkaClientState#NOT_INITIALIZED}
     * @throws RuntimeException      - if any system error occurred
     * @throws Error                 - if any system error occurred
     */
    void initSync();

    /**
     * Asynchronously creates kafka clients
     *
     * @throws IllegalStateException - if current client state is not {@link MaasKafkaClientState#INITIALIZED} or {@link MaasKafkaClientState#INACTIVE}
     *                               (will be passed as exception parameter to handle function)
     * @throws RuntimeException      - if any system error occurred
     * @throws Error                 - if any system error occurred
     *
     * @return completable future instance
     */
    CompletableFuture<Void> activateAsync();

    /**
     * Synchronously creates kafka clients
     *
     * @throws IllegalStateException - if current client state is not {@link MaasKafkaClientState#INITIALIZED} or {@link MaasKafkaClientState#INACTIVE}
     * @throws RuntimeException      - if any system error occurred
     * @throws Error                 - if any system error occurred
     */
    void activateSync();

    /**
     * Registers state change listener
     * @param listener instance
     */
    void addChangeStateListener(MaasKafkaClientStateChangeListener listener);

    /**
     * Unregisters state change listener
     * @param listener instance
     */
   void removeChangeStateListener(MaasKafkaClientStateChangeListener listener);
}
