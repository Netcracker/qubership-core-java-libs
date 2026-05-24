package com.netcracker.cloud.dbaas.client.restclient.webclient;

import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.okhttp.MicroserviceOkHttpRestClient;
import com.netcracker.cloud.security.core.auth.M2MManager;
import com.netcracker.cloud.security.core.utils.k8s.M2MClientFactory;
import com.netcracker.cloud.smartclient.config.annotation.EnableFrameworkWebClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.function.Consumer;

@Configuration
@EnableFrameworkWebClient
public class DbaasWebClientConfiguration {

    @Value("${security.m2m.kubernetes.enabled:false}")
    private boolean k8sEnabled;

    @Bean("dbaasRestClient")
    public MicroserviceRestClient dbaasRestClient(M2MManager m2MManager) {
        var client = M2MClientFactory.getDbaasOkHttpClient(() -> m2MManager.getToken().getTokenValue(), k8sEnabled);
        return new MicroserviceOkHttpRestClient(client);
    }

    // If sleuth enabled, it tries to get db health from http filters. But dataSource can be not initialized yet.
    // Disable it explicitly for dbaasRestClient
    public static class DisableHttpTraceFilterConsumer implements Consumer<List<ExchangeFilterFunction>> {
        public static final String HTTP_FILTER_CLASS_PACKAGE_TO_REMOVE = "org.springframework.cloud.sleuth.instrument.web.client";

        @Override
        public void accept(List<ExchangeFilterFunction> exchangeFilterFunctions) {
            exchangeFilterFunctions.removeIf(f -> f.getClass().getPackage().getName().equals(HTTP_FILTER_CLASS_PACKAGE_TO_REMOVE));
        }
    }

}
