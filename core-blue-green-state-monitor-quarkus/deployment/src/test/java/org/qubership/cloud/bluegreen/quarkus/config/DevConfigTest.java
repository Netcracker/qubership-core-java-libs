package org.qubership.cloud.bluegreen.quarkus.config;


import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.api.service.GlobalMutexService;
import org.qubership.cloud.bluegreen.api.service.MicroserviceMutexService;
import org.qubership.cloud.bluegreen.quarkus.deployment.BlueGreenStateMonitorProcessor;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class DevConfigTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(BlueGreenStateMonitorProcessor.class)
                    .addAsResource("application-dev-default.properties", "application.properties")
            );

    @Inject
    BlueGreenStatePublisher blueGreenStatePublisher;
    @Inject
    GlobalMutexService globalMutexService;
    @Inject
    MicroserviceMutexService microserviceMutexService;

    @Test
    void testBlueGreenStatePublisherBean() {
        Assertions.assertNotNull(blueGreenStatePublisher);
//        Assertions.assertTrue(blueGreenStatePublisher instanceof InMemoryBlueGreenStatePublisher);
    }

    @Test
    void testGlobalMutexServiceBean() {
        Assertions.assertNotNull(globalMutexService);
//        Assertions.assertTrue(globalMutexService instanceof InMemoryGlobalMutexService);
    }

    @Test
    void testMicroserviceMutexServiceBean() {
        Assertions.assertNotNull(microserviceMutexService);
//        Assertions.assertTrue(microserviceMutexService instanceof InMemoryMicroserviceMutexService);
    }
}
