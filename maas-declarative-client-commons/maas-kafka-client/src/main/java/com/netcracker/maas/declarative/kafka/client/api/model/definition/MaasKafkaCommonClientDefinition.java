package org.qubership.maas.declarative.kafka.client.api.model.definition;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Map;
import java.util.Optional;

public abstract class MaasKafkaCommonClientDefinition {
    private MaasTopicDefinition topic;
    private boolean isTenant;
    private Map<String, Object> clientConfig;

    public MaasKafkaCommonClientDefinition(MaasTopicDefinition topic, boolean isTenant, Map<String, Object> clientConfig) {
        this.topic = topic;
        this.isTenant = isTenant;
        this.clientConfig = clientConfig;
    }

    public MaasTopicDefinition getTopic() {
        return topic;
    }

    public boolean isTenant() {
        return isTenant;
    }

    public Map<String, Object> getClientConfig() {
        return Optional.ofNullable(clientConfig).orElse(Map.of());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("topic", topic)
                .append("isTenant", isTenant)
                .append("clientConfig", clientConfig)
                .toString();
    }
}
