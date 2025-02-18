package org.qubership.cloud.security.core.utils.tls;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTlsConfigTest {
    @Test
    void testSslContextLoad() {
        assertNotNull(new DefaultTlsConfig().getSslContext());
    }

    @Test
    void testGetTrustManagers() {
        assertNotNull(new DefaultTlsConfig().getTrustManager());
    }
}