package org.qubership.maas.declarative.kafka.spring.client.config.local.dev;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static org.qubership.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants.KAFKA_LOCAL_DEV_ENABLED;


@Component
@ConfigurationProperties("maas.kafka.local-dev")
@ConditionalOnProperty(value = KAFKA_LOCAL_DEV_ENABLED, havingValue = "true")
public class MaasKafkaLocalDevConfigProps {

    Map<String, String> config;

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public Map<String, Object> getAsStringObjectMap() {
        Map<String, Object> objectMap = new HashMap<>();
        if (config != null) {
            for (String key : config.keySet()) {
                objectMap.put(key, config.get(key));
            }
        }
        return objectMap;
    }
}
