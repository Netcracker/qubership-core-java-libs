package com.netcracker.maas.declarative.kafka.client.api;

/**
 * Describes kafka client state
 */
public enum MaasKafkaClientState {
    NOT_INITIALIZED,
    INITIALIZING,// state for proper initializing
    INITIALIZED,
    ACTIVE,
    INACTIVE
}
