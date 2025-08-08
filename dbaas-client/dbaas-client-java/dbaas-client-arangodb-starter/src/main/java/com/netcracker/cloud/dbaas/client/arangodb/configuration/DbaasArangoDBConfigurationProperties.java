package org.qubership.cloud.dbaas.client.arangodb.configuration;

import com.arangodb.config.ArangoConfigProperties;
import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

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

    @Bean("arangodbDbaasApiProperties")
    @ConfigurationProperties("dbaas.api.arangodb")
    public DbaasApiProperties dbaasApiProperties() {
        return new DbaasApiProperties();
    }
}
