package org.qubership.cloud.quarkus.dbaas.opensearch.client.config;

import org.qubership.cloud.quarkus.dbaas.opensearch.client.config.properties.metrics.OpensearchMetricsProperties;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import lombok.Getter;

import java.util.Optional;

@Getter
@ConfigRoot(name = "dbaas.opensearch", phase = ConfigPhase.RUN_TIME)
public class DbaaSOpensearchConfigurationProperty {
    /**
     * Property with opensearch SSL mode for database connection.
     */
    @ConfigItem(name = "ssl", defaultValue = "AUTO")
    public SSLMode sslMode;

    /**
     * Properties for Opensearch client metrics.
    */
    @ConfigItem(name = "metrics")
    public OpensearchMetricsProperties metrics;

    public enum SSLMode {
        AUTO,
        ENABLE,
        DISABLE
    }

    /**
     * Property with maxConnTotal value for PoolingAsyncClientConnectionManagerBuilder.
     */
    @ConfigItem(name = "max-conn-total")
    public Optional<Integer> maxConnTotal;

    /**
     * Property with maxConnPerRoute value for PoolingAsyncClientConnectionManagerBuilder.
     */
    @ConfigItem(name = "max-conn-per-route")
    public Optional<Integer> maxConnPerRoute;
}
