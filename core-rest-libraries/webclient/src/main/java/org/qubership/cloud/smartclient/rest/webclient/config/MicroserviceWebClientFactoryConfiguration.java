package org.qubership.cloud.smartclient.rest.webclient.config;

import org.qubership.cloud.context.propagation.spring.webclient.annotation.EnableWebclientContextProvider;
import org.qubership.cloud.restclient.MicroserviceRestClientFactory;
import org.qubership.cloud.restclient.webclient.MicroserviceWebClientFactory;
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