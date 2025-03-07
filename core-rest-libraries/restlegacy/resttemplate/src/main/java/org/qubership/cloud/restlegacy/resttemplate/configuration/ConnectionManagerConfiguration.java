package org.qubership.cloud.restlegacy.resttemplate.configuration;

import org.qubership.cloud.restlegacy.resttemplate.configuration.customizer.PoolingHttpClientConnectionManagerCustomizer;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Optional;

@Configuration
public class ConnectionManagerConfiguration {

    @Value("${core.connection.manager.maxConnectionsPerRoute:#{null}}")
    private Integer maxConnectionsPerRoute;

    @Value("${core.connection.manager.maxConnectionsTotal:#{null}}")
    private Integer maxConnectionsTotal;

    @Value("${connection.readTimeout:60000}")
    private int readTimeout;

    @Bean(name = "coreConnectionManager")
    @Primary
    HttpClientConnectionManager poolingHttpClientConnectionManager(
            Optional<PoolingHttpClientConnectionManagerCustomizer> poolingHttpClientConnectionManagerCustomizer) {

        SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(Timeout.ofMilliseconds(readTimeout)).build();
        final PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultSocketConfig(socketConfig)
                .build();
        if (maxConnectionsTotal != null) {
            poolingHttpClientConnectionManager.setMaxTotal(maxConnectionsTotal);
        }
        if (maxConnectionsPerRoute != null) {
            poolingHttpClientConnectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        }
        poolingHttpClientConnectionManagerCustomizer.ifPresent(poolingHttpClientConnectionManagerObj ->
                poolingHttpClientConnectionManagerObj.customize(poolingHttpClientConnectionManager));
        return poolingHttpClientConnectionManager;
    }
}
