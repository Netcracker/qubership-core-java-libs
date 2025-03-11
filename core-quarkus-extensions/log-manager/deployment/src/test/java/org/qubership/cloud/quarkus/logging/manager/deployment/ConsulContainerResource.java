package org.qubership.cloud.quarkus.logging.manager.deployment;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.Map;

public class ConsulContainerResource implements QuarkusTestResourceLifecycleManager {

    ConsulContainer consulContainer;

    @Override
    public Map<String, String> start() {
        consulContainer = new ConsulContainer("hashicorp/consul:1.15")
                .withConsulCommand("kv put logging/test-namespace/test-app/logging/level/com/example/cloud FINE");
        consulContainer.start();

        return Map.of("CONSUL_URL", String.format("http://%s:%s", consulContainer.getHost(), consulContainer.getFirstMappedPort()));
    }

    @Override
    public void stop() {
        if (consulContainer != null) {
            consulContainer.stop();
        }
    }
}
