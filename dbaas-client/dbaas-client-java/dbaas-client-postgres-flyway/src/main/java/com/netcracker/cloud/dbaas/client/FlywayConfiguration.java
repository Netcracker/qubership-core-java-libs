package com.netcracker.cloud.dbaas.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
public class FlywayConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "dbaas.postgres")
    public FlywayConfigurationProperties flywayConfigurationProperties(){
        return new FlywayConfigurationProperties();
    }


    @Bean("flywayPostgresPostProcessor")
    @ConditionalOnMissingBean(name = "flywayPostgresPostProcessor")
    public FlywayPostgresPostProcessor flywayPostgresPostProcessor(FlywayConfigurationProperties flywayConfigurationProperties){
        return new FlywayPostgresPostProcessor(flywayConfigurationProperties);
    }
}
