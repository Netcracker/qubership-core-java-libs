package org.qubership.cloud.security.core.utils.tls;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TlsUtilsTest {

    @Test
    void testServiceLoaderFromCurrentThreadContextClassLoader() {
        ServiceLoader<TlsConfig> serviceLoader = ServiceLoader.load(TlsConfig.class);

        assertNotNull(serviceLoader);

        int count = 0;
        for (TlsConfig service : serviceLoader) {
            assertNotNull(service, "Failed to load service.");
            count++;
        }

        assertEquals(2, count, "Failed to load all services.");

        Assertions.assertNotNull(TlsUtils.getSslContext());
    }
}
