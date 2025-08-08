package org.qubership.maas.declarative.kafka.quarkus.client.test;

import com.google.common.collect.ImmutableMap;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig;

import java.util.Map;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig.defaultClusterConfig;

public class TestConfiguration implements QuarkusTestResourceLifecycleManager {

    private EmbeddedKafkaCluster kafka;

    @Override
    public Map<String, String> start() {
        EmbeddedKafkaClusterConfig kafkaClusterConfig = defaultClusterConfig();
        kafka = provisionWith(kafkaClusterConfig);
        kafka.start();
        return ImmutableMap.<String, String>builder()
                .put("quarkus.consul-source-config.agent.url", "localhost:8080")
                .put("maas.kafka.local-dev.enabled", "true")
                .put("maas.kafka.local-dev.tenant-ids", "00000000-0000-0000-0000-000000000000")
                .put("maas.kafka.local-dev.config.bootstrap.servers", "localhost:9092")
                .build();
    }

    @Override
    public void stop() {
        if (kafka != null) {
            kafka.stop();
        }
    }
}
