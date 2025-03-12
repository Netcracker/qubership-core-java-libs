package org.qubership.cloud.bluegreen.quarkus.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

public class BlueGreenBuildTimeConfig {

    @ConfigRoot(prefix = "blue-green", name = "global-mutex-service", phase = ConfigPhase.BUILD_TIME)
    public static class GlobalMutex {
        /**
         * Enables Blue Green Global Mutex Service
         */
        @ConfigItem(name = "enabled", defaultValue = "true")
        public boolean enabled;
    }

    @ConfigRoot(prefix = "blue-green", name = "microservice-mutex-service", phase = ConfigPhase.BUILD_TIME)
    public static class MicroserviceMutex {
        /**
         * Enables Blue Green Microservice Mutex Service
         */
        @ConfigItem(name = "enabled", defaultValue = "true")
        public boolean enabled;
    }

    @ConfigRoot(prefix = "blue-green", name = "state-publisher", phase = ConfigPhase.BUILD_TIME)
    public static class StatePublisher {
        /**
         * Enables Blue Green State Publisher
         */
        @ConfigItem(name = "enabled", defaultValue = "true")
        public boolean enabled;
    }

}
