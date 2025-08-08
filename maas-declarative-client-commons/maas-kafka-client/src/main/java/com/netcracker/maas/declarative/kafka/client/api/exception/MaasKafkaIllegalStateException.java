package org.qubership.maas.declarative.kafka.client.api.exception;

public class MaasKafkaIllegalStateException extends MaasKafkaException {

    public MaasKafkaIllegalStateException(String message) {
        super(message);
    }

    public MaasKafkaIllegalStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
