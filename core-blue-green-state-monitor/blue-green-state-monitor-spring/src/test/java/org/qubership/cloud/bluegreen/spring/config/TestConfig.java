package org.qubership.cloud.bluegreen.spring.config;

import org.springframework.context.annotation.Import;

@Import({InMemoryConfig.class,
        BlueGreenGlobalMutexConfiguration.class,
        BlueGreenMicroserviceMutexConfiguration.class,
        BlueGreenStatePublisherConfiguration.class})
public class TestConfig {
}
