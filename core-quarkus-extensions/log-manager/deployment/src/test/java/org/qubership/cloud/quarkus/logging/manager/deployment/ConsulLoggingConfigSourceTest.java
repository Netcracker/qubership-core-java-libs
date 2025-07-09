package org.qubership.cloud.quarkus.logging.manager.deployment;

import org.qubership.cloud.log.manager.common.LogManager;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.quarkus.consul.client.ConsulSourceConfig;
import org.qubership.cloud.quarkus.logging.manager.runtime.consul.ConsulLoggingConfigWatchFactory;
import org.qubership.cloud.quarkus.logging.manager.runtime.consul.ConsulLoggingSourceConfig;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(ConsulContainerResource.class)
class ConsulLoggingConfigSourceTest {
    @Inject
    ConsulLoggingConfigWatchFactory consulLoggingConfigWatchFactory;

    @Test
    void testPropertyLoadedFromConsul() throws Exception {
        ConsulSourceConfig consulSourceConfig = new ConsulSourceConfig() {
            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public AgentConfig agent() {
                return null;
            }

            @Override
            public Optional<List<String>> propertiesRoot() {
                return Optional.empty();
            }

            @Override
            public Integer waitTime() {
                return 570;
            }
        };
        ConsulLoggingSourceConfig consulLoggingSourceConfig = new ConsulLoggingSourceConfig() {
            @Override
            public boolean loggingEnabled() {
                return true;
            }

            @Override
            public Integer consulRetryTime() {
                return 20000;
            }
        };

        consulLoggingConfigWatchFactory.initConsulLoggingWatch(new StartupEvent(), consulSourceConfig, consulLoggingSourceConfig);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<String> levelFuture = executorService.submit(() -> {
            String level;
            do {
                level = LogManager.getLogLevel().get("com.example.cloud");
                Thread.sleep(100);
            } while (level == null);
            return level;
        });
        String level = levelFuture.get(5, TimeUnit.SECONDS);
        assertEquals(Level.FINE.toString(), level);
    }
}
