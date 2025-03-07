package org.qubership.cloud.restlegacy.resttemplate.configuration;

import org.qubership.cloud.restclient.MicroserviceRestClientFactory;
import org.qubership.cloud.restclient.resttemplate.MicroserviceRestTemplateFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MicroserviceRestTemplateFactoryConfiguration {
    @Bean("simpleMicroserviceRestClientFactory")
    @ConditionalOnMissingBean
    public MicroserviceRestClientFactory getMicroserviceRestClientFactory() {
        return new MicroserviceRestTemplateFactory();
    }
}