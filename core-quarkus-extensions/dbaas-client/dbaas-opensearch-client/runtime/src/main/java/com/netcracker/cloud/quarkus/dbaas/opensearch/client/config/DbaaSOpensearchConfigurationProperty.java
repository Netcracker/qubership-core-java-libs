package com.netcracker.cloud.quarkus.dbaas.opensearch.client.config;

import com.netcracker.cloud.quarkus.dbaas.opensearch.client.config.properties.metrics.OpensearchMetricsProperties;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.Optional;

@ConfigMapping(prefix = "quarkus.dbaas.opensearch")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DbaaSOpensearchConfigurationProperty {

    /**
     * Property with opensearch SSL mode for database connection.
     */
    @WithName("ssl")
    @WithDefault("AUTO")
    SSLMode sslMode();

    /**
     * Properties for Opensearch client metrics.
     */
    @WithName("metrics")
    OpensearchMetricsProperties metrics();

    enum SSLMode {
        AUTO,
        ENABLE,
        DISABLE
    }

    /**
     * Property with maxConnTotal value for PoolingAsyncClientConnectionManagerBuilder.
     */
    @WithName("max-conn-total")
    Optional<Integer> maxConnTotal();

    /**
     * Property with maxConnPerRoute value for PoolingAsyncClientConnectionManagerBuilder.
     */
    @WithName("max-conn-per-route")
    Optional<Integer> maxConnPerRoute();
}
