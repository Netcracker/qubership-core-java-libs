package org.qubership.cloud.restlegacy.resttemplate.connection.manager;

import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.restlegacy.resttemplate.configuration.ConnectionManagerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {ConnectionManagerConfiguration.class, TestConnectionManagerCustomizerImpl.class})
class PoolingHttpClientConnectionManagerCustomizerTest {
    @Autowired
    @Qualifier("coreConnectionManager")
    HttpClientConnectionManager httpClientConnectionManager;

    @Test
    void maxTotalTest() {
        Assertions.assertEquals(13, ((PoolingHttpClientConnectionManager) httpClientConnectionManager).getMaxTotal());
    }

    @Test
    void maxPerRoutTest() {
        Assertions.assertEquals(9, ((PoolingHttpClientConnectionManager) httpClientConnectionManager).getDefaultMaxPerRoute());
    }
}
