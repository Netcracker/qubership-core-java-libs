package org.qubership.maas.declarative.kafka.quarkus.client.impl;

import org.qubership.maas.declarative.kafka.client.impl.definition.api.MaasKafkaClientConfigPlatformService;
import org.qubership.maas.declarative.kafka.quarkus.client.ConfigUtils;
import org.eclipse.microprofile.config.Config;

import java.util.Map;

public class MaasKafkaClientConfigPlatformServiceImpl implements MaasKafkaClientConfigPlatformService {

    private final Config config;

    public MaasKafkaClientConfigPlatformServiceImpl(Config config) {
        this.config = config;
    }

    @Override
    public Map<String, Object> getClientConfigByPrefix(String prefix) {
        return ConfigUtils.configAsMap(config, prefix);
    }
}
