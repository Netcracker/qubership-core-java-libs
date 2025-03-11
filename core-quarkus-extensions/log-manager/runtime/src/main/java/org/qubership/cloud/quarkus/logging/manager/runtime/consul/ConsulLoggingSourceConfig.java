package org.qubership.cloud.quarkus.logging.manager.runtime.consul;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.consul-logger-watcher")
public interface ConsulLoggingSourceConfig {
    String DEFAULT_CONSUL_RETRY_TIME = "20000";

    /**
     * Is logging levels watch enabled
     */
    @WithDefault("true")
    boolean loggingEnabled();

    /**
     * Retry time if consul is not available
     */
    @WithDefault(DEFAULT_CONSUL_RETRY_TIME)
    Integer consulRetryTime();

}
