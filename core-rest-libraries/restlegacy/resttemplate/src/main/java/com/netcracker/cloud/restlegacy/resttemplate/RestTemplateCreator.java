package com.netcracker.cloud.restlegacy.resttemplate;


import com.netcracker.cloud.security.common.restclient.OAuthRestTemplateProvider;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public class RestTemplateCreator {
    @NotNull
    private final ClientHttpRequestFactory requestFactory;
    @NotNull
    private final OAuthRestTemplateProvider oAuthRestTemplateProvider;
    private final RestTemplateBuilder springRestTemplateBuilder;
    @NotNull
    @NotNull
    private final List<ClientHttpRequestInterceptor> clientHttpRequestInterceptors = new ArrayList<>();
    private boolean m2m;

    RestTemplateCreator(@NotNull OAuthRestTemplateProvider oAuthRestTemplateProvider,
                        @NotNull List<ClientHttpRequestInterceptor> defaultClientHttpRequestInterceptors,
                        @NotNull ClientHttpRequestFactory requestFactory,
                        RestTemplateBuilder springRestTemplateBuilder) {
        this.oAuthRestTemplateProvider = oAuthRestTemplateProvider;
        this.springRestTemplateBuilder = springRestTemplateBuilder;
        this.clientHttpRequestInterceptors.addAll(defaultClientHttpRequestInterceptors);
        this.requestFactory = requestFactory;
    }

    @NotNull
    public RestTemplateCreator withM2M() {
        this.m2m = true;
        return this;
    }

    @NotNull
    public RestTemplate create() {
        return getRestTemplate();
    }



    @NotNull
    private RestTemplate getRestTemplate() {
        RestTemplate restTemplate;
        if (m2m) {
            restTemplate = createM2MRestTemplate();
        } else {
            restTemplate = createRegularRestTemplate();
        }
        if (!clientHttpRequestInterceptors.isEmpty()) {
            restTemplate.setInterceptors(clientHttpRequestInterceptors);
        }
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }

    private RestTemplate createRegularRestTemplate() {
        return springRestTemplateBuilder.build();
    }

    private RestTemplate createM2MRestTemplate() {
        return oAuthRestTemplateProvider.getOAuthRestTemplate();
    }

}
