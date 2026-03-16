package com.netcracker.maas.declarative.kafka.quarkus.client.test;

import com.google.common.collect.ImmutableMap;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

@Testcontainers
public class TestConfiguration implements QuarkusTestResourceLifecycleManager {

    @Container
    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(DockerImageName.parse("apache/kafka"));

    @Override
    public Map<String, String> start() {
        KAFKA_CONTAINER.start();
        return ImmutableMap.<String, String>builder()
                .put("quarkus.consul-source-config.agent.url", "localhost:8080")
                .put("maas.kafka.local-dev.enabled", "true")
                .put("maas.kafka.local-dev.tenant-ids", "00000000-0000-0000-0000-000000000000")
                .put("maas.kafka.local-dev.config.bootstrap.servers", KAFKA_CONTAINER.getBootstrapServers())
                .build();
    }

    @Override
    public void stop() {
        KAFKA_CONTAINER.stop();
    }
}
