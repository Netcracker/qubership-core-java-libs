package com.netcracker.maas.declarative.kafka.spring.client.impl;

import org.qubership.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants;
import org.qubership.maas.declarative.kafka.client.impl.definition.api.MaasKafkaClientConfigPlatformService;
import org.qubership.maas.declarative.kafka.spring.client.config.MaasKafkaClientConfigKeeper;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static org.qubership.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants.KAFKA_CONSUMER_PREFIX;
import static org.qubership.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants.KAFKA_PRODUCER_PREFIX;


public class MaasKafkaClientConfigPlatformServiceImpl implements MaasKafkaClientConfigPlatformService {

    private final MaasKafkaClientConfigKeeper configKeeper;

    public MaasKafkaClientConfigPlatformServiceImpl(MaasKafkaClientConfigKeeper configKeeper) {
        this.configKeeper = configKeeper;
    }

    @Override
    public Map<String, Object> getClientConfigByPrefix(String prefix) {
        if (prefix != null) {
            if (prefix.startsWith(MaasKafkaCommonConstants.CONSUMER_PREFIX)) {
                Map<String, Map<String, String>> consumer = configKeeper.getClient().get("consumer");
                String key = prefix.substring(MaasKafkaCommonConstants.CONSUMER_PREFIX.length());
                return getStringObjectMap(consumer, key, KAFKA_CONSUMER_PREFIX);
            } else if (prefix.startsWith(MaasKafkaCommonConstants.PRODUCER_PREFIX)) {
                Map<String, Map<String, String>> producer = configKeeper.getClient().get("producer");
                String key = prefix.substring(MaasKafkaCommonConstants.PRODUCER_PREFIX.length());
                return getStringObjectMap(producer, key, KAFKA_PRODUCER_PREFIX);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @NotNull
    private Map<String, Object> getStringObjectMap(Map<String, Map<String, String>> sourceMap, String key, String skipPrefix) {
        if (key.endsWith(".")) {
            key = key.substring(0, key.length() - 1);
        }

        Map<String, String> toBeProcessedMap = sourceMap.get(key);
        Map<String, Object> precessedMap = new HashMap<>();
        for (String mapKey : toBeProcessedMap.keySet()) {
            if (mapKey.startsWith(skipPrefix)) {
                precessedMap.put(
                        mapKey,
                        toBeProcessedMap.get(mapKey) == null ? null : toBeProcessedMap.get(mapKey).trim()
                );
            } else {
                precessedMap.put(
                        mapKey,
                        processValue(
                                toBeProcessedMap.get(mapKey)
                        )
                );
            }
        }

        return precessedMap;
    }


    private Object processValue(String val) {
        if (val == null) {
            return null;
        }

        String trimmed = val.trim();
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            // Ignore it
        }

        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            // Ignore it
        }

        if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
            return Boolean.parseBoolean(trimmed);
        }

        return trimmed;
    }
}
