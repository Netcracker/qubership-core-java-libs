package org.qubership.maas.declarative.kafka.client.api.exception;

public class MaasKafkaTopicDefinitionBuildException extends MaasKafkaException {

    public MaasKafkaTopicDefinitionBuildException(String format, Object ...args) {
        super(String.format(format, args));
    }
}
