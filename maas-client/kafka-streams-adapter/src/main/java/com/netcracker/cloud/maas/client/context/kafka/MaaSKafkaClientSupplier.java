package com.netcracker.cloud.maas.client.context.kafka;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ForwardingAdmin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.streams.KafkaClientSupplier;

import java.util.Map;
import java.util.function.Function;

public class MaaSKafkaClientSupplier implements KafkaClientSupplier {
    private final Function<Map<String, Object>, ForwardingAdmin> adminSupplier;

    public MaaSKafkaClientSupplier(Function<Map<String, Object>, ForwardingAdmin> adminSupplier) {
        this.adminSupplier = adminSupplier;
    }

    @Override
    public Admin getAdmin(Map<String, Object> config) {
        return adminSupplier.apply(config);
    }

    @Override
    public Producer<byte[], byte[]> getProducer(final Map<String, Object> config) {
        return new KafkaProducer<>(config, new ByteArraySerializer(), new ByteArraySerializer());
    }

    @Override
    public Consumer<byte[], byte[]> getConsumer(final Map<String, Object> config) {
        return new KafkaConsumer<>(config, new ByteArrayDeserializer(), new ByteArrayDeserializer());
    }

    @Override
    public Consumer<byte[], byte[]> getRestoreConsumer(final Map<String, Object> config) {
        return new KafkaConsumer<>(config, new ByteArrayDeserializer(), new ByteArrayDeserializer());
    }

    @Override
    public Consumer<byte[], byte[]> getGlobalConsumer(final Map<String, Object> config) {
        return new KafkaConsumer<>(config, new ByteArrayDeserializer(), new ByteArrayDeserializer());
    }
}
