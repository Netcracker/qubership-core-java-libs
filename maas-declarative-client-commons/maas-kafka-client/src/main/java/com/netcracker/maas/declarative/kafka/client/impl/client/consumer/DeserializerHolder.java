package org.qubership.maas.declarative.kafka.client.impl.client.consumer;

import org.apache.kafka.common.serialization.Deserializer;

public class DeserializerHolder {

    private Deserializer keyDeserializer;
    private Deserializer valueDeserializer;

    public DeserializerHolder(Deserializer keyDeserializer, Deserializer valueDeserializer) {
        this.keyDeserializer = keyDeserializer;
        this.valueDeserializer = valueDeserializer;
    }

    public Deserializer getKeyDeserializer() {
        return keyDeserializer;
    }

    public Deserializer getValueDeserializer() {
        return valueDeserializer;
    }
}
