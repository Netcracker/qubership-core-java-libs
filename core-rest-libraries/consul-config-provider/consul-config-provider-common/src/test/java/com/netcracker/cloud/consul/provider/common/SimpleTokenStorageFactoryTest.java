package com.netcracker.cloud.consul.provider.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SimpleTokenStorageFactoryTest {

    @Test
    void basedOnSimpleTokenStorage() {
        TokenStorage tokenStorage = new SimpleTokenStorageFactory(null).createTokenStorage(null);
        Assertions.assertTrue(tokenStorage instanceof SimpleTokenStorage);
    }
}
