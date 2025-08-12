package com.netcracker.maas.declarative.kafka.client.api.exception;

public class MaasKafkaConsumerDefinitionBuildException extends MaasKafkaException {

    public MaasKafkaConsumerDefinitionBuildException(String format, Object ...args) {
        super(String.format(format, args));
    }
}
