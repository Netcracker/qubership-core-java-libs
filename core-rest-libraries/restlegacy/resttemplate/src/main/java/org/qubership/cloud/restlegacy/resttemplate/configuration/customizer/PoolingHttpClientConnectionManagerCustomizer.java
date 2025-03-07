package org.qubership.cloud.restlegacy.resttemplate.configuration.customizer;

import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;


public interface PoolingHttpClientConnectionManagerCustomizer {
    void customize(PoolingHttpClientConnectionManager connectionManager);
}
