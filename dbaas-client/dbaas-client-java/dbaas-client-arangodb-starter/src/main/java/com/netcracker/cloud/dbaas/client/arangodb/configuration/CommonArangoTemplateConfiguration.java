package com.netcracker.cloud.dbaas.client.arangodb.configuration;

import com.arangodb.serde.ArangoSerde;
import com.arangodb.springframework.config.ArangoConfiguration;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import org.qubership.cloud.dbaas.client.arangodb.util.ArangoTemplateCreationUtils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Proxy;

@Configuration
public class CommonArangoTemplateConfiguration {

    @Bean
    public ArangoSerde arangoSerde() throws Exception {
        return ArangoTemplateCreationUtils.getInstance().serde();
    }

    @Bean
    public ArangoMappingContext arangoMappingContext() throws Exception {
        return ArangoTemplateCreationUtils.getInstance().arangoMappingContext();
    }

    @Bean
    public ArangoConverter arangoConverter() throws Exception {
        return ArangoTemplateCreationUtils.getInstance().arangoConverter();
    }

    @Bean
    public ArangoConfiguration arangoConfiguration() {
        return (ArangoConfiguration) Proxy.newProxyInstance(ArangoConfiguration.class.getClassLoader(), new Class[]{ArangoConfiguration.class}, (proxy, method, args) -> method.invoke(ArangoTemplateCreationUtils.getInstance(), args));
    }
}
