package com.netcracker.cloud.restlegacy.resttemplate.connection.manager;

import com.netcracker.cloud.restlegacy.resttemplate.configuration.customizer.PoolingHttpClientConnectionManagerCustomizer;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.stereotype.Component;

@Component
public class TestConnectionManagerCustomizerImpl implements PoolingHttpClientConnectionManagerCustomizer {
    @Override
    public void customize(PoolingHttpClientConnectionManager connectionManager) {
        connectionManager.setMaxTotal(13);
        connectionManager.setDefaultMaxPerRoute(9);
    }
}
