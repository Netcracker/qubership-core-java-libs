package org.qubership.maas.declarative.kafka.client.api.exception;

public class MaasKafkaNoResourceException extends MaasKafkaException {

    public MaasKafkaNoResourceException(String message) {
        super(message);
    }

    public MaasKafkaNoResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
