package com.netcracker.cloud.dbaas.client.arangodb.configuration;

import com.arangodb.config.ArangoConfigProperties;
import com.netcracker.cloud.dbaas.client.entity.DbaasApiProperties;
import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.netcracker.cloud.dbaas.client.arangodb.configuration.DbaasArangoConfigPropertiesImpl.DEFAULT_TIMEOUT_MS;

@ConfigurationProperties(prefix = "dbaas")
@Configuration
public class DbaasArangoDBConfigurationProperties {

    @Getter
    @Setter
    private Map<String, String> arangodb = new HashMap<>();
    private ArangoConfigProperties arangoConfigProperties;

    public ArangoConfigProperties asArangoConfigProperties() {
        if (arangoConfigProperties == null) {
            arangoConfigProperties = new DbaasArangoConfigPropertiesImpl(arangodb);
        }
        return arangoConfigProperties;
    }

    public long checkConnectionTimeoutMs() {
        return asArangoConfigProperties().getTimeout().filter(t -> t > 0).orElse(DEFAULT_TIMEOUT_MS).longValue();
    }

    @Bean("arangodbDbaasApiProperties")
    @ConfigurationProperties("dbaas.api.arangodb")
    public DbaasApiProperties dbaasApiProperties() {
        return new DbaasApiProperties();
    }
}
