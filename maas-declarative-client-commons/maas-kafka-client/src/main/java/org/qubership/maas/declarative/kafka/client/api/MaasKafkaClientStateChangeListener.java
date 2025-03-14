package org.qubership.maas.declarative.kafka.client.api;

/**
 * Interface described client maas kafka client state change listener
 */
public interface MaasKafkaClientStateChangeListener {

    /**
     * Method invoked when client state change occurred
     * @param oldState state changing from
     * @param newState state changing to
     */
    void onChange(MaasKafkaClientState oldState, MaasKafkaClientState newState);
}
