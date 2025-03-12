package org.qubership.cloud.bluegreen.quarkus.config;


import org.qubership.cloud.bluegreen.api.service.GlobalMutexService;
import org.qubership.cloud.bluegreen.quarkus.deployment.BlueGreenStateMonitorProcessor;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class BlueGreenGlobalConfigTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(BlueGreenStateMonitorProcessor.class)
                    .addAsResource("application-global-default.properties", "application.properties")
            );

    @Inject
    GlobalMutexService globalMutexService;

    @Test
    void testGlobalMutexService() {
        Assertions.assertNotNull(globalMutexService);
    }
}
