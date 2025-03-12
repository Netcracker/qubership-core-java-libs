package org.qubership.cloud.bluegreen.quarkus.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

public class InMemoryBlueGreenBuildTimeConfig {

    @ConfigRoot(prefix = "blue-green", phase = ConfigPhase.BUILD_TIME)
    public static class BlueGreenGlobal {
        /**
         * Enables Blue Green Global Mutex Service
         */
        @ConfigItem(name = "enabled", defaultValue = "true")
        public boolean enabled;
    }

    @ConfigRoot(prefix = "blue-green", name = "state-monitor.dev", phase = ConfigPhase.BUILD_TIME)
    public static class Dev {
        /**
         * Enables Blue Green Microservice Mutex Service
         */
        @ConfigItem(name = "enabled", defaultValue = "false")
        public boolean enabled;
    }

}
