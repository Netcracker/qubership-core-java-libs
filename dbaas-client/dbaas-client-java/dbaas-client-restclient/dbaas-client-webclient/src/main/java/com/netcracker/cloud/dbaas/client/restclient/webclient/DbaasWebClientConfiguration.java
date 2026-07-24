package com.netcracker.cloud.dbaas.client.restclient.webclient;

import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.okhttp.MicroserviceOkHttpRestClient;
import com.netcracker.cloud.security.core.auth.DummyM2MManager;
import com.netcracker.cloud.security.core.auth.M2MManager;
import com.netcracker.cloud.security.core.utils.k8s.M2MClientFactory;
import com.netcracker.cloud.smartclient.config.annotation.EnableFrameworkWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import java.util.List;
import java.util.function.Consumer;

@Configuration
@EnableFrameworkWebClient
public class DbaasWebClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DbaasWebClientConfiguration.class);

    @Bean("dbaasRestClient")
    public MicroserviceRestClient dbaasRestClient(M2MManager m2MManager) {
        var client = M2MClientFactory.getDbaasOkHttpClient(() -> m2MManager.getToken().getTokenValue());
        return new MicroserviceOkHttpRestClient(client);
    }

    @Bean
    @ConditionalOnMissingBean(M2MManager.class)
    public M2MManager dbaasWebClientM2MManager() {
        log.warn("Initialized dummy m2m manager for dbaas web client. Do not use in production.");
        return new DummyM2MManager();
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
