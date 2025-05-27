package org.qubership.cloud.restlegacy.resttemplate.connection.manager;

import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.restlegacy.resttemplate.configuration.ConnectionManagerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {ConnectionManagerConfiguration.class},
        properties = {
                "core.connection.manager.maxConnectionsPerRoute=12",
                "core.connection.manager.maxConnectionsTotal=22"
        }
)
class ConnectionManagerConfigurationTest {

    @Autowired
    @Qualifier("coreConnectionManager")
    HttpClientConnectionManager httpClientConnectionManager;

    @Test
    void maxTotalTest() {
        assertEquals(22, ((PoolingHttpClientConnectionManager) httpClientConnectionManager).getMaxTotal());
    }

    @Test
    void maxPerRoutTest() {
        assertEquals(12, ((PoolingHttpClientConnectionManager) httpClientConnectionManager).getDefaultMaxPerRoute());
    }
}
