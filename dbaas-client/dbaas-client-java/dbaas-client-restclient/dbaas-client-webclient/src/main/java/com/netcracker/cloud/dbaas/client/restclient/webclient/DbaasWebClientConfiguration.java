package com.netcracker.cloud.dbaas.client.restclient.webclient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.webclient.MicroserviceWebClient;
import com.netcracker.cloud.smartclient.config.annotation.EnableFrameworkWebClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.function.Consumer;

@Configuration
@EnableFrameworkWebClient
public class DbaasWebClientConfiguration {

    @Bean("dbaasRestClient")
    public MicroserviceRestClient dbaasRestClient(@Qualifier("m2mWebClient") WebClient webClient) {
        WebClient customizedWebClient = webClient.mutate()
                .filters(new DisableHttpTraceFilterConsumer())
                .codecs(clientCodecsConfigurer -> clientCodecsConfigurer.defaultCodecs()
                        .configureDefaultCodec(o -> {
                            if (o instanceof Jackson2JsonDecoder decoder) {
                                decoder.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                            }
                        }))
                .build();

        return new MicroserviceWebClient(customizedWebClient);
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
