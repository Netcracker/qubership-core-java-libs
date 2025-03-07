package org.qubership.cloud.consul.provider.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TokenStorageFactoryTest {

    private final String NAMESPACE = "ns";
    private final String CONSUL_URL = "http://consul.namespace:8500";

    @Test
    void returnsFine() {
        TokenStorageFactory.CreateOptions.Builder builder = new TokenStorageFactory.CreateOptions.Builder();
        Assertions.assertNotNull(builder);
        TokenStorageFactory.CreateOptions opts = builder.consulUrl(CONSUL_URL)
                .namespace(NAMESPACE)
                .m2mSupplier(() -> "token")
                .build();
        Assertions.assertNotNull(opts);
        Assertions.assertEquals(NAMESPACE, opts.namespace);
        Assertions.assertEquals(CONSUL_URL, opts.consulUrl);
    }

    @Test
    void cannotBuildWithoutUrlOrNamespaceOrM2MSupplier() {
        Assertions.assertThrows(NullPointerException.class, () -> new TokenStorageFactory.CreateOptions.Builder()
                .namespace(NAMESPACE)
                .m2mSupplier(() -> "token")
                .build());
        Assertions.assertThrows(NullPointerException.class, () -> new TokenStorageFactory.CreateOptions.Builder()
                .consulUrl(CONSUL_URL)
                .m2mSupplier(() -> "token")
                .build());
        Assertions.assertThrows(NullPointerException.class, () -> new TokenStorageFactory.CreateOptions.Builder()
                .consulUrl(CONSUL_URL)
                .namespace(NAMESPACE)
                .build());
    }

}
