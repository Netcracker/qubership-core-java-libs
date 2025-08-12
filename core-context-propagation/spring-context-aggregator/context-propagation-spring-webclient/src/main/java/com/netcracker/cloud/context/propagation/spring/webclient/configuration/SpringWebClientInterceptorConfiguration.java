package com.netcracker.cloud.context.propagation.spring.webclient.configuration;

import com.netcracker.cloud.context.propagation.spring.webclient.interceptor.CoreContextPropagator;
import com.netcracker.cloud.context.propagation.spring.webclient.interceptor.SpringWebClientInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class SpringWebClientInterceptorConfiguration {
    @Bean
    public SpringWebClientInterceptor springWebClientInterceptor(){
        return new SpringWebClientInterceptor();
    }

    @PostConstruct
    public void enableContextPropagation() {
        CoreContextPropagator.installHook();
    }
}
