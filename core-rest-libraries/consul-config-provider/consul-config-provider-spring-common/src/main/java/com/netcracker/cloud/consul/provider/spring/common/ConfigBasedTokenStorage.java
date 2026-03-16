package com.netcracker.cloud.consul.provider.spring.common;

import com.netcracker.cloud.consul.provider.common.TokenStorage;
import org.springframework.cloud.consul.config.ConsulConfigProperties;

public class ConfigBasedTokenStorage implements TokenStorage {

    private final ConsulConfigProperties configProperties;

    public ConfigBasedTokenStorage(ConsulConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    @Override
    public String get() {
        return configProperties.getAclToken();
    }

    @Override
    public void update(String token) {
        configProperties.setAclToken(token);
    }
}
