package org.qubership.maas.declarative.kafka.client.impl.local.dev.config.impl;

import org.qubership.maas.declarative.kafka.client.impl.local.dev.config.api.MaasKafkaLocalDevConfigProviderService;

import java.util.Map;

// Localdev only
public class MaasKafkaLocalDevConfigProviderImpl implements MaasKafkaLocalDevConfigProviderService {

    private final Map<String , Object> kafkaConfig;

    public MaasKafkaLocalDevConfigProviderImpl(Map<String, Object> kafkaConfig) {
        // TODO process kafka config?
        this.kafkaConfig = kafkaConfig;
    }

    @Override
    public Map<String, Object> get() {
        return kafkaConfig;
    }
}
