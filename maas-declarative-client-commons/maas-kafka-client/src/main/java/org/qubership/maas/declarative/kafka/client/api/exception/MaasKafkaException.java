package org.qubership.maas.declarative.kafka.client.api.exception;

public class MaasKafkaException extends RuntimeException {

    public MaasKafkaException(String message) {
        super(message);

    }

    public MaasKafkaException(String message, Throwable cause) {
        super(message, cause);
    }
}
