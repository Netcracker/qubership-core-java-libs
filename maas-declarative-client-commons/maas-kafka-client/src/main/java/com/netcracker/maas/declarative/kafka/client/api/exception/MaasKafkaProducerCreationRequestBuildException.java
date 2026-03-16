package com.netcracker.maas.declarative.kafka.client.api.exception;

public class MaasKafkaProducerCreationRequestBuildException extends MaasKafkaException {

    public MaasKafkaProducerCreationRequestBuildException(String format, Object... args) {
        super(String.format(format, args));
    }
}
