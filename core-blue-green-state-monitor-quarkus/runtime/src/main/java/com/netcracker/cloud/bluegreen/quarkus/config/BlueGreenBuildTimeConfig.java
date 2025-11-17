package com.netcracker.cloud.bluegreen.quarkus.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "blue-green")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface BlueGreenBuildTimeConfig {

    /**
     * Configuration for the Blue Green Global Mutex Service.
     * Maps properties under `blue-green.global-mutex-service`.
     */
    @WithName("global-mutex-service")
    GlobalMutex globalMutex();

    /**
     * Configuration for the Blue Green Microservice Mutex Service.
     * Maps properties under `blue-green.microservice-mutex-service`.
     */
    @WithName("microservice-mutex-service")
    MicroserviceMutex microserviceMutex();

    /**
     * Configuration for the Blue Green State Publisher.
     * Maps properties under `blue-green.state-publisher`.
     */
    @WithName("state-publisher")
    StatePublisher statePublisher();

    interface GlobalMutex {
        /**
         * Enables Blue Green Global Mutex Service
         */
        @WithDefault("true")
        boolean enabled();
    }

    interface MicroserviceMutex {
        /**
         * Enables Blue Green Microservice Mutex Service
         */
        @WithDefault("true")
        boolean enabled();
    }

    interface StatePublisher {
        /**
         * Enables Blue Green State Publisher
         */
        @WithDefault("true")
        boolean enabled();
    }
}
