package com.netcracker.cloud.consul.provider.spring.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.consul.config.ConsulConfigProperties;

class ConfigBasedTokenStorageTest {

    @Test
    void tokenSetProxiesToConfig() {
        ConsulConfigProperties consulConfigProperties = new ConsulConfigProperties();
        ConfigBasedTokenStorage tokenStorage = new ConfigBasedTokenStorage(consulConfigProperties);
        tokenStorage.update("new-token");

        Assertions.assertEquals("new-token", consulConfigProperties.getAclToken());
        Assertions.assertEquals(tokenStorage.get(), consulConfigProperties.getAclToken());
    }
}
