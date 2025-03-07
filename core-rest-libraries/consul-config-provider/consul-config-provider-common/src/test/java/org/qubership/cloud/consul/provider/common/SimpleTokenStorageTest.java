package org.qubership.cloud.consul.provider.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SimpleTokenStorageTest {

    @Test
    void emptyTokenOnInstanceCreation() {
        SimpleTokenStorage tokenStorage = new SimpleTokenStorage();
        Assertions.assertEquals("", tokenStorage.get());
    }

    @Test
    void tokenSet() {
        SimpleTokenStorage tokenStorage = new SimpleTokenStorage();
        tokenStorage.update("my-token");
        Assertions.assertEquals("my-token", tokenStorage.get());
    }

    @Test
    void emptyTokenIsAllowed() {
        SimpleTokenStorage tokenStorage = new SimpleTokenStorage("init-token");
        Assertions.assertEquals("init-token", tokenStorage.get());

        tokenStorage.update("");
        Assertions.assertEquals("", tokenStorage.get());
    }
}
