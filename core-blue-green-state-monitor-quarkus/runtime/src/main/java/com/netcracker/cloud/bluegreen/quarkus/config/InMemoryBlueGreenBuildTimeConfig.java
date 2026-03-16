package com.netcracker.cloud.bluegreen.quarkus.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "blue-green")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface InMemoryBlueGreenBuildTimeConfig {

    /**
     * Global Blue-Green configuration.
     */
    @WithParentName
    BlueGreenGlobal global();

    /**
     * Dev Blue-Green configuration.
     */
    @WithName("state-monitor.dev")
    Dev dev();

    interface BlueGreenGlobal {
        /**
         * Enables Blue Green Global Mutex Service
         */
        @WithDefault("true")
        boolean enabled();
    }

    interface Dev {
        /**
         * Enables Blue Green Microservice Mutex Service
         */
        @WithDefault("false")
        boolean enabled();
    }
}
