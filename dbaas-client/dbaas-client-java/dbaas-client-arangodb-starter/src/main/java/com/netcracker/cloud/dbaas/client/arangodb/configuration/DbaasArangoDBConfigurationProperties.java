package com.netcracker.cloud.dbaas.client.arangodb.configuration;

import com.arangodb.config.ArangoConfigProperties;
import com.netcracker.cloud.dbaas.client.entity.DbaasApiProperties;
import com.netcracker.cloud.dbaas.client.management.ArangoDatabaseProvider;
import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    /**
     * Deadline for the liveness probe used by {@code ArangoDatabaseProvider}/{@code DbaasArangoTemplate}
     * before considering a connection unhealthy and retrying. Deliberately a separate knob from
     * {@code dbaas.arangodb.timeout} (the driver's own connect/request timeout): this value multiplies
     * by every retry, so it needs its own, much smaller default.
     */
    public long checkConnectionTimeoutMs() {
        return Optional.ofNullable(arangodb.get("connectionCheckTimeout"))
                .map(Long::valueOf)
                .filter(t -> t > 0)
                .orElse(ArangoDatabaseProvider.DEFAULT_CONNECTION_CHECK_TIMEOUT_MS);
    }

    @Bean("arangodbDbaasApiProperties")
    @ConfigurationProperties("dbaas.api.arangodb")
    public DbaasApiProperties dbaasApiProperties() {
        return new DbaasApiProperties();
    }
}
