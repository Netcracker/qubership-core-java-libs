package org.qubership.cloud.restlegacy.resttemplate.connection.manager;

import org.qubership.cloud.restlegacy.resttemplate.configuration.ConnectionManagerConfiguration;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ConnectionManagerConfiguration.class})
@TestPropertySource(properties = {
        "core.connection.manager.maxConnectionsPerRoute=12",
        "core.connection.manager.maxConnectionsTotal=22"
})
public class ConnectionManagerConfigurationTest {

    @Autowired
    @Qualifier("coreConnectionManager")
    HttpClientConnectionManager httpClientConnectionManager;


    @Test
    public void maxTotalTest(){
        Assertions.assertEquals(22,((PoolingHttpClientConnectionManager) httpClientConnectionManager).getMaxTotal());
    }

    @Test
    public void maxPerRoutTest(){
        Assertions.assertEquals(12,((PoolingHttpClientConnectionManager) httpClientConnectionManager).getDefaultMaxPerRoute());
    }

}
