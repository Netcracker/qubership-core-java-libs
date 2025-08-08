package org.qubership.cloud.maas.client.api.kafka;

import org.qubership.cloud.maas.client.api.Classifier;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

// Test API stability
public class TopicAddressTest {
    @Test
    public void testAPI() {
        var ta = new TopicAddress() {
            @Override
            public Classifier getClassifier() {
                return null;
            }

            @Override
            public String getTopicName() {
                return "";
            }

            @Override
            public String getBoostrapServers(String protocol) {
                return "";
            }

            @Override
            public Optional<TopicUserCredentials> getCredentials(String type) {
                return Optional.empty();
            }

            @Override
            public String getCACert() {
                return "";
            }

            @Override
            public int getNumPartitions() {
                return 0;
            }

            @Override
            public Optional<Map<String, Object>> formatConnectionProperties() {
                return Optional.empty();
            }

            @Override
            public boolean isVersioned() {
                return false;
            }
        };

        // test that added getConfigs() doesn't require implementing in subclasses
        assertThrows(UnsupportedOperationException.class, () -> ta.getConfigs());
    }
}
