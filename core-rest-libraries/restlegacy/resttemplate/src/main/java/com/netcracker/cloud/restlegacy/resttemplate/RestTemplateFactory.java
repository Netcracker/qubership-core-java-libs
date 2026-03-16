package com.netcracker.cloud.restlegacy.resttemplate;

import com.netcracker.cloud.security.common.restclient.OAuthRestTemplateProvider;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Factory which stores rest templates built with alternative RestTemplateFactory
 * but does not expose them as beans to prevent not single candidates for autowiring
 */
public class RestTemplateFactory {
    private RestTemplate restTemplate;

    private RestTemplate m2mRestTemplate;

    @NotNull
    private final OAuthRestTemplateProvider oAuthRestTemplateProvider;
    @NotNull
    private final List<ClientHttpRequestInterceptor> clientHttpRequestInterceptors;
    private final RestTemplateBuilder springRestTemplateBuilder;
    @NotNull
    private final ClientHttpRequestFactory requestFactory;

    public RestTemplateFactory(@NotNull OAuthRestTemplateProvider oAuthRestTemplateProvider,
                               @NotNull ClientHttpRequestFactory requestFactory,
                               @NotNull List<ClientHttpRequestInterceptor> clientHttpRequestInterceptors,
                               RestTemplateBuilder springRestTemplateBuilder) {
        this.oAuthRestTemplateProvider = oAuthRestTemplateProvider;
        this.requestFactory = requestFactory;
        this.clientHttpRequestInterceptors = clientHttpRequestInterceptors;
        this.springRestTemplateBuilder = springRestTemplateBuilder;
    }

    private RestTemplate restTemplate() {
        return getRestTemplateCreator().create();
    }

    private RestTemplate m2mRestTemplate() {
        return getRestTemplateCreator().withM2M().create();
    }


    public RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = this.restTemplate();
        }
        return restTemplate;
    }

    public RestTemplate getM2mRestTemplate() {
        if (m2mRestTemplate == null) {
            m2mRestTemplate = this.m2mRestTemplate();

            setDefaultJacksonConverter(m2mRestTemplate);
        }
        return m2mRestTemplate;
    }

    private void setDefaultJacksonConverter(RestTemplate m2mRestTemplate) {
        List<HttpMessageConverter<?>> messageConverters = m2mRestTemplate.getMessageConverters();
        for (int i = 0; i < messageConverters.size(); i++) {
            HttpMessageConverter<?> messageConverter = messageConverters.get(i);
            if (messageConverter instanceof MappingJackson2HttpMessageConverter) {
                messageConverters.set(i, new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json().build()));
                break;
            }
        }
        m2mRestTemplate.setMessageConverters(messageConverters);
    }

    public RestTemplateCreator getRestTemplateCreator() {
        return new RestTemplateCreator(oAuthRestTemplateProvider,
                clientHttpRequestInterceptors,
                requestFactory,
                springRestTemplateBuilder);
    }
}
