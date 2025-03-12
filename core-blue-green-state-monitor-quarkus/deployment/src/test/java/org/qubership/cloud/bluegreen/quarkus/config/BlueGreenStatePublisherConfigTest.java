package org.qubership.cloud.bluegreen.quarkus.config;


import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.quarkus.deployment.BlueGreenStateMonitorProcessor;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class BlueGreenStatePublisherConfigTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(BlueGreenStateMonitorProcessor.class)
                    .addAsResource("application-publisher-default.properties", "application.properties")
            );

    @Inject
    BlueGreenStatePublisher blueGreenStatePublisher;

    @Test
    void testBlueGreenStatePublisher() {
        Assertions.assertNotNull(blueGreenStatePublisher);
    }
}
