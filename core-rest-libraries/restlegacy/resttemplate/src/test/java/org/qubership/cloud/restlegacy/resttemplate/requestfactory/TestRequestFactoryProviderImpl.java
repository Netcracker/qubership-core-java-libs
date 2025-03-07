package org.qubership.cloud.restlegacy.resttemplate.requestfactory;

import org.qubership.cloud.restlegacy.resttemplate.configuration.customizer.RequestFactoryProvider;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;

@Component
public class TestRequestFactoryProviderImpl implements RequestFactoryProvider {

    @Override
    public ClientHttpRequestFactory provide(HttpClientBuilder httpClientBuilder) {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClientBuilder.build());
        return (new BufferingClientHttpRequestFactory(requestFactory));
    }
}
