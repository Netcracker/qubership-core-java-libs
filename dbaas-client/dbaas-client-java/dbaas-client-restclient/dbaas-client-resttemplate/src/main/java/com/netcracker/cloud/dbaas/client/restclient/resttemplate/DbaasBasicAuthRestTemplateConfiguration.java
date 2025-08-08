package org.qubership.cloud.dbaas.client.restclient.resttemplate;

import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.resttemplate.MicroserviceRestTemplate;
import org.qubership.cloud.security.core.utils.tls.TlsUtils;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.ssl.TLS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnProperty(value = "dbaas.restclient.resttemplate.basic-auth", havingValue = "true")
public class DbaasBasicAuthRestTemplateConfiguration {

    @Bean("dbaasRestClient")
    public MicroserviceRestClient dbaasRestClientBasicAuth(@Value("${dbaas.api.username}") String dbaasUsername,
                                                           @Value("${dbaas.api.password}") String dbaasPassword) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(dbaasUsername, dbaasPassword));
        final SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(TlsUtils.getSslContext())
                .setTlsVersions(TLS.V_1_1, TLS.V_1_2, TLS.V_1_3)
                .build();
        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .build();
        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
        return new MicroserviceRestTemplate(restTemplate);
    }

}
