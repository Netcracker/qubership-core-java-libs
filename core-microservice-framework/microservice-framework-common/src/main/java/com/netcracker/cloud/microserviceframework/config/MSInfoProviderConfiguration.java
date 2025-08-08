package com.netcracker.cloud.microserviceframework.config;

import org.qubership.cloud.dbaas.client.config.MSInfoProvider;
import org.qubership.cloud.microserviceframework.dataaccess.LocalMSInfoProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MSInfoProviderConfiguration {
    @Bean
    MSInfoProvider msInfoProvider(){
        return new LocalMSInfoProvider();
    }
}
