package org.qubership.maas.declarative.kafka.client.impl.client.consumer.executor;

public enum ExecutorState {
    INACTIVE, // start state
    ACTIVE,
    SUSPENDED,
    CLOSED
}
