package org.qubership.cloud.consul.config.source;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.consul.ConsulContainer;

import java.util.Map;

public class ConsulContainerResource implements QuarkusTestResourceLifecycleManager {

    ConsulContainer consulContainer;

    @Override
    public Map<String, String> start() {
        consulContainer = new ConsulContainer("hashicorp/consul:1.15")
                .withConsulCommand("kv put config/test-namespace/test-app/test/consul/property value-from-consul");
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
