package org.qubership.cloud.quarkus.consul.client;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.validation.constraints.Max;

import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "quarkus.consul-source-config")
public interface ConsulSourceConfig {
    int MAX_WAIT_TIME = 570;
    String DEFAULT_WAIT_TIME = "570";

    /**
     * If set to true, the application will attempt to look up the configuration from Consul
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Consul agent related configuration
     */
    AgentConfig agent();

    /**
     * Common prefix that all keys share when looking up the keys from Consul.
     * In case of a key conflict, the key value from the last root will be used.
     */
    Optional<List<String>> propertiesRoot();

    /**
     * Consul Blocking Queries wait time.
     */
    @Max(MAX_WAIT_TIME)
    @WithDefault(DEFAULT_WAIT_TIME)
    Integer waitTime();

    interface AgentConfig {

        /**
         * Consul agent URL
         */
        Optional<String> url();
    }
}
