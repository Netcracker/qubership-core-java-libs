package com.netcracker.cloud.smartclient.rest.webclient.config;

import com.netcracker.cloud.context.propagation.spring.webclient.annotation.EnableWebclientContextProvider;
import com.netcracker.cloud.restclient.MicroserviceRestClientFactory;
import com.netcracker.cloud.restclient.webclient.MicroserviceWebClientFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableWebclientContextProvider
public class MicroserviceWebClientFactoryConfiguration {
    @Bean("simpleMicroserviceRestClientFactory")
    @ConditionalOnMissingBean
    public MicroserviceRestClientFactory getMicroserviceRestClientFactory() {
        return new MicroserviceWebClientFactory();
    }
}
