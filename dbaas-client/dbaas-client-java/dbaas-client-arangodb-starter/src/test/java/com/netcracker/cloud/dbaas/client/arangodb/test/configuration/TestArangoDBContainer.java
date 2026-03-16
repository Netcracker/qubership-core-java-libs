package com.netcracker.cloud.dbaas.client.arangodb.test.configuration;

import com.arangodb.ArangoDB;
import jakarta.annotation.PreDestroy;

import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.concurrent.TimeUnit;

import static com.netcracker.cloud.dbaas.client.arangodb.test.ArangoTestCommon.*;

public class TestArangoDBContainer extends GenericContainer<TestArangoDBContainer> {
    private static final DockerImageName ARANGODB_IMAGE = DockerImageName.parse("arangodb:3.11.10");
    private static final String ARANGO_INIT_SCRIPT_LOCATION = "/docker-entrypoint-initdb.d/init.js";

    static final String RYUK_CONTAINER_IMAGE_PROPERTY = "ryuk.container.image";
    static final String RYUK_CONTAINER_IMAGE = "testcontainers/ryuk:0.11.0";

    static {
        TestcontainersConfiguration configs = TestcontainersConfiguration.getInstance();
        configs.updateUserConfig(RYUK_CONTAINER_IMAGE_PROPERTY, RYUK_CONTAINER_IMAGE);
    }

    private static TestArangoDBContainer arangoDBTestContainer;

    private TestArangoDBContainer() {
        super(ARANGODB_IMAGE);
    }

    @PreDestroy
    public void destroy() {
        arangoDBTestContainer.stop();
    }

    public static TestArangoDBContainer getInstance() {
        if (arangoDBTestContainer == null) {
            arangoDBTestContainer = new TestArangoDBContainer()
                    .withExposedPorts(DB_PORT)
                    .withEnv("ARANGO_ROOT_PASSWORD", "root")
                    .withCopyToContainer(MountableFile.forClasspathResource("arangodb/init.js"), ARANGO_INIT_SCRIPT_LOCATION)
                    .waitingFor(new WaitForDBReadyStrategy());
        }
        return arangoDBTestContainer;
    }

    private static class WaitForDBReadyStrategy extends AbstractWaitStrategy {

        @Override
        protected void waitUntilReady() {

            String host = waitStrategyTarget.getHost();
            Integer port = waitStrategyTarget.getMappedPort(DB_PORT);

            try {
                Unreliables.retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                    getRateLimiter().doWhenReady(() -> {
                        ArangoDB arangoDB = new ArangoDB.Builder()
                                .host(host, port)
                                .user(TEST_USER)
                                .password(TEST_PASSWORD)
                                .build();
                        try {
                            if (!arangoDB.db(TestArangoDBConfiguration.DB_NAME_1).exists()) {
                                throw new RuntimeException(String.format("Database %s is not created yet", TestArangoDBConfiguration.DB_NAME_1));
                            }
                            if (!arangoDB.db(TestArangoDBConfiguration.DB_NAME_2).exists()) {
                                throw new RuntimeException(String.format("Database %s is not created yet", TestArangoDBConfiguration.DB_NAME_2));
                            }
                        } finally {
                            arangoDB.shutdown();
                        }
                    });
                    return true;
                });
            } catch (TimeoutException e) {
                throw new ContainerLaunchException("Timed out waiting for ArangoDB to get ready");
            }
        }
    }
}
