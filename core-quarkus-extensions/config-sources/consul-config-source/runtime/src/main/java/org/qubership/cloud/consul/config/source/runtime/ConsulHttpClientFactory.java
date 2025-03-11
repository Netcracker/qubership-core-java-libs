package org.qubership.cloud.consul.config.source.runtime;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

class ConsulHttpClientFactory {

    /**
     * Defaults from {@link com.ecwid.consul.transport.DefaultHttpTransport}
     */
    static final int DEFAULT_MAX_CONNECTIONS = 1000;
    static final int DEFAULT_MAX_PER_ROUTE_CONNECTIONS = 500;
    static final int DEFAULT_CONNECTION_TIMEOUT_MS = 10 * 1000; // 10 sec
    static final int DEFAULT_READ_TIMEOUT_MS = 1000 * 60 * 10; // 10 min

    static HttpClient get(int consulWaitTimeSeconds) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(DEFAULT_MAX_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE_CONNECTIONS);

        RequestConfig requestConfig = RequestConfig.custom().
                setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT_MS).
                setConnectionRequestTimeout(DEFAULT_CONNECTION_TIMEOUT_MS).
                setSocketTimeout(getIncreasedReadSocketTimeoutMillis(1000 * consulWaitTimeSeconds)).
                build();

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().
                setConnectionManager(connectionManager).
                setDefaultRequestConfig(requestConfig).
                useSystemProperties();

        return httpClientBuilder.build();
    }

    static int getIncreasedReadSocketTimeoutMillis(int valueMillis) {
        // Consul documentation says it adds `wait + random(0, wait / 16)`, and maximum is 10 minutes
        // https://www.consul.io/api-docs/features/blocking#blocking-queries
        final int epsMillis = DEFAULT_READ_TIMEOUT_MS / 10;

        if (valueMillis >= (DEFAULT_READ_TIMEOUT_MS - epsMillis)) {
            return valueMillis + epsMillis;
        }
        return DEFAULT_READ_TIMEOUT_MS;
    }
}
