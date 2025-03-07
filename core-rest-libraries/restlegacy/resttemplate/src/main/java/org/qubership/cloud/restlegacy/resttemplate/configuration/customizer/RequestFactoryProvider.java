package org.qubership.cloud.restlegacy.resttemplate.configuration.customizer;

import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;

public interface RequestFactoryProvider {
    ClientHttpRequestFactory provide(HttpClientBuilder httpClientBuilder);
}
