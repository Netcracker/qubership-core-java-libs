package com.netcracker.maas.declarative.kafka.spring.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties("maas.kafka")
public class MaasKafkaClientConfigKeeper {

    Map<String, Map<String, Map<String, String>>> client;

    public Map<String, Map<String, Map<String, String>>> getClient() {
        return client;
    }

    public void setClient(Map<String, Map<String, Map<String, String>>> client) {
        this.client = client;
    }

}
