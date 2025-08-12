package com.netcracker.maas.declarative.kafka.client.api.exception;

public class MaasKafkaConsumerCreationRequestBuildException extends MaasKafkaException {

    public MaasKafkaConsumerCreationRequestBuildException(String format, Object... args) {
        super(String.format(format, args));
    }
}
