package com.netcracker.cloud.consul.provider.spring.common;

import com.netcracker.cloud.consul.provider.common.TokenStorage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SpringTokenStorageFactoryTest {

    @Test
    void basedOnConfigBasedTokenStorage() {
        TokenStorage tokenStorage = new SpringTokenStorageFactory(null, null)
                .createTokenStorage(null);
        Assertions.assertTrue(tokenStorage instanceof ConfigBasedTokenStorage);
    }
}
