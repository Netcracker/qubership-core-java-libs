package com.netcracker.cloud.dbaas.client.opensearch;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

public interface DbaasOpensearchClientBuilderCustomizer {
    default void customize(ApacheHttpClient5TransportBuilder builder) {};

    default void customize(HttpAsyncClientBuilder builder) {};

    default void customize(RequestConfig.Builder builder) {};

    default void customize(PoolingAsyncClientConnectionManagerBuilder connectionManagerBuilder) {};

}
