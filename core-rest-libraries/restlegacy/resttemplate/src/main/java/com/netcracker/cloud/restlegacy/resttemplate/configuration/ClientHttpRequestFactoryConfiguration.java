package com.netcracker.cloud.restlegacy.resttemplate.configuration;

import com.netcracker.cloud.restlegacy.resttemplate.configuration.customizer.RequestFactoryProvider;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.Optional;

@Configuration
@Import(ConnectionManagerConfiguration.class)
public class ClientHttpRequestFactoryConfiguration {

    @Value("${connection.readTimeout:60000}")
    private int readTimeout;

    @Value("${connection.connectTimeout:60000}")
    private int connectTimeout;

    @Value("${connection.connectionRequestTimeout:60000}")
    private int connectionRequestTimeout;

    @Bean(name = "clientHttpRequestFactory")
    public ClientHttpRequestFactory httpComponentsClientHttpRequestFactory(@Qualifier("coreConnectionManager") HttpClientConnectionManager httpClientConnectionManager,
                                                                           Optional<RequestFactoryProvider> requestFactoryProvider) {
        HttpClientBuilder httpClientBuilder;
        httpClientBuilder = HttpClients.custom().setConnectionManager(httpClientConnectionManager);

        if (requestFactoryProvider.isPresent()) {
            return requestFactoryProvider.get().provide(httpClientBuilder);
        } else {
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setConnectionRequestTimeout(connectionRequestTimeout);
            requestFactory.setConnectTimeout(connectTimeout);
            requestFactory.setHttpClient(httpClientBuilder.build());
            return requestFactory;
        }
    }

    @Bean(name = "simpleClientHttpRequestFactory")
    public SimpleClientHttpRequestFactory simpleClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(readTimeout);
        requestFactory.setConnectTimeout(connectTimeout);
        return requestFactory;
    }
}
