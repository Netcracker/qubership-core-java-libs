package org.qubership.cloud.consul.config.source;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(ConsulContainerResource.class)
class ConsulConfigSourceTest {
    @ConfigProperty(name = "test.consul.property")
    String consulProperty;

    @Test
    void testPropertyLoadedFromConsul() {
        Assertions.assertEquals("value-from-consul", consulProperty);
    }

}
