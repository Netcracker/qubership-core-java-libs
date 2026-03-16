package com.netcracker.maas.declarative.kafka.client.api.exception;

public class MaasKafkaMandatoryPropertyAbsentException extends MaasKafkaException {

    public MaasKafkaMandatoryPropertyAbsentException(String format, Object ...args) {
        super(String.format(format, args));
    }
}
