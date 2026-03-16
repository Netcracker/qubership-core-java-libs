package com.netcracker.cloud.smartclient.rest.webclient.config;

import jakarta.annotation.PostConstruct;
import com.netcracker.cloud.context.propagation.spring.webclient.annotation.EnableWebclientContextProvider;
import com.netcracker.cloud.context.propagation.spring.webclient.interceptor.CoreContextPropagator;
import com.netcracker.cloud.context.propagation.spring.webclient.interceptor.SpringWebClientInterceptor;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.webclient.MicroserviceWebClient;
import com.netcracker.cloud.security.common.webclient.AuthorizationHeaderInterceptorFactory;
import com.netcracker.cloud.security.common.webclient.DefaultSmartWebClient;
import com.netcracker.cloud.security.common.webclient.SmartWebClient;
import com.netcracker.cloud.smartclient.rest.webclient.interceptor.RequestLoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;

@EnableWebclientContextProvider
@Configuration
@Import(MicroserviceWebClientFactoryConfiguration.class)
public class WebClientConfiguration {

    private static final String WEB_CLIENT_INTERCEPTORS_ROOT_PACKAGE_NAME = "com.netcracker.cloud.smartclient.rest.webclient.interceptor";
    private static final String SECURITY_INTERCEPTORS_ROOT_PACKAGE_NAME = "com.netcracker.cloud.securitycore.rest.webclient.interceptor";

    @Bean
    public RequestLoggingInterceptor requestLoggingInterceptor() {
        return new RequestLoggingInterceptor();
    }


    @Bean
    public SmartWebClient smartClient(List<ExchangeFilterFunction> exchangeFilterFunctionMap,
                                      @Autowired(required = false) @Qualifier("securityContextPropagatorInterceptor") ExchangeFilterFunction securityContextPropagatorInterceptor,
                                      SpringWebClientInterceptor springWebClientInterceptor,
                                      AuthorizationHeaderInterceptorFactory authorizationHeaderInterceptorFactory,
                                      WebClient.Builder builder) {
        List<ExchangeFilterFunction> interceptors = exchangeFilterFunctionMap
                .stream()
                .filter(exchangeFilterFunction -> {
                    String packageName = exchangeFilterFunction.getClass().getPackage().getName();
                    return packageName.startsWith(WEB_CLIENT_INTERCEPTORS_ROOT_PACKAGE_NAME)
                            || packageName.startsWith(SECURITY_INTERCEPTORS_ROOT_PACKAGE_NAME);
                })
                .collect(Collectors.toList());
        interceptors.add(springWebClientInterceptor);
        return new DefaultSmartWebClient(interceptors,
                securityContextPropagatorInterceptor,
                authorizationHeaderInterceptorFactory,
                builder);
    }

    @Bean
    @ConditionalOnMissingBean(AuthorizationHeaderInterceptorFactory.class)
    public AuthorizationHeaderInterceptorFactory dummyAuthorizationHeaderInterceptorFactory() {
        return mode -> (request, next) -> next.exchange(request);
    }

    @Bean
    @ConditionalOnMissingBean(name = "securityContextPropagatorInterceptor")
    public ExchangeFilterFunction dummySecurityContextPropagatorInterceptor() {
        return (request, next) -> next.exchange(request);
    }

    @Bean("m2mRestClient")
    public MicroserviceRestClient m2mRestClient(SmartWebClient smartWebClient) {
        return new MicroserviceWebClient(smartWebClient.getWebClientForM2mAuthorization());
    }

    @Bean("m2mWebClient")
    public WebClient m2mWebClient(SmartWebClient smartWebClient) {
        return smartWebClient.getWebClientForM2mAuthorization();
    }

    @Bean("userWebClient")
    public WebClient userWebClient(SmartWebClient smartWebClient) {
        return smartWebClient.getWebClientForUserAuthorization();
    }

    @Bean("smartWebClient")
    public WebClient smartWebClient(SmartWebClient smartWebClient) {
        return smartWebClient.getWebClientBuilder().build();
    }

    @PostConstruct
    public void enableContextPropagation() {
        CoreContextPropagator.installHook();
    }
}
