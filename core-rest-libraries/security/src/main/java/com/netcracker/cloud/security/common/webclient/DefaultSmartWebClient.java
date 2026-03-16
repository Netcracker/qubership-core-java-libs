package com.netcracker.cloud.security.common.webclient;

import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.function.UnaryOperator;

import static java.util.function.UnaryOperator.identity;

public class DefaultSmartWebClient implements SmartWebClient {
    private final List<ExchangeFilterFunction> genericInterceptors;
    private final WebClient.Builder builder;
    private final ExchangeFilterFunction m2mAuthorizationHeader;
    private final ExchangeFilterFunction userAuthorizationHeader;
    private final ExchangeFilterFunction securityContextPropagatorInterceptor;

    public DefaultSmartWebClient(List<ExchangeFilterFunction> genericInterceptors,
                                 ExchangeFilterFunction securityContextPropagatorInterceptor,
                                 AuthorizationHeaderInterceptorFactory authorizationHeaderInterceptorFactory,
                                 WebClient.Builder builder) {
        this.genericInterceptors = genericInterceptors;
        this.builder = builder;
        this.m2mAuthorizationHeader = authorizationHeaderInterceptorFactory.build(Mode.M2M);
        this.userAuthorizationHeader = authorizationHeaderInterceptorFactory.build(Mode.USER);
        this.securityContextPropagatorInterceptor = securityContextPropagatorInterceptor;
    }

    public WebClient.Builder getWebClientBuilderForMode(Mode mode) {
        WebClient.Builder webClientBuilder;
        switch (mode) {
            case DEFAULT:
                if (securityContextPropagatorInterceptor == null) {
                    webClientBuilder = prepareWebClient();
                } else {
                    webClientBuilder = prepareWebClient(exchangeFilterFunction -> securityContextPropagatorInterceptor)
                            .filter(securityContextPropagatorInterceptor);
                }
                break;
            case M2M:
                webClientBuilder = prepareWebClient()
                        .filter(m2mAuthorizationHeader);
                break;
            case USER:
                webClientBuilder = prepareWebClient()
                        .filter(userAuthorizationHeader);
                break;
            default:
                webClientBuilder = prepareWebClient();
                break;
        }
        return webClientBuilder;
    }

    /**
     * Depending on authorization context builds default or m2m webClient.
     * <p>
     * Default WebClient would use user JWT token from current user session to issue new requests.
     * M2M WebClient is capable of fetching new m2m tokens from IdP (only Keycloak is supported) and
     * refreshing them on expiration.
     */
    public WebClient.Builder getWebClientBuilder() {
        return getWebClientBuilderForMode(Mode.DEFAULT);
    }

    public WebClient getWebClientForM2mAuthorization() {
        return getWebClientBuilderForMode(Mode.M2M)
                .build();
    }

    public WebClient getWebClientForUserAuthorization() {
        return getWebClientBuilderForMode(Mode.USER)
                .build();
    }

    private WebClient.Builder prepareWebClient() {
        return prepareWebClient(identity());
    }

    private WebClient.Builder prepareWebClient(UnaryOperator<ExchangeFilterFunction> wrapper) {
        return builder.clone()
                .filters(filters -> genericInterceptors.stream()
                        .map(wrapper)
                        .forEach(filters::add));
    }
}
