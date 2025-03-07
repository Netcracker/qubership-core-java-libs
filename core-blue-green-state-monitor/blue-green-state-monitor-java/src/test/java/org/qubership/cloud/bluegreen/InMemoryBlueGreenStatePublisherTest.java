package org.qubership.cloud.bluegreen;

import org.qubership.cloud.bluegreen.api.model.BlueGreenState;
import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.model.Version;
import org.qubership.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.qubership.cloud.bluegreen.impl.util.EnvUtil.NAMESPACE_PROP;

class InMemoryBlueGreenStatePublisherTest {

    @Test
    void testResolveNamespaceFromProp() {
        String initial = System.getProperty(NAMESPACE_PROP);
        System.setProperty(NAMESPACE_PROP, "test-ns-via-prop");
        try {
            InMemoryBlueGreenStatePublisher statePublisher = new InMemoryBlueGreenStatePublisher();
            BlueGreenState blueGreenState = statePublisher.getBlueGreenState();
            Assertions.assertEquals("test-ns-via-prop", blueGreenState.getCurrent().getNamespace());
        } finally {
            if (initial == null) {
                System.clearProperty(NAMESPACE_PROP);
            } else {
                System.setProperty(NAMESPACE_PROP, initial);
            }
        }
    }

    @Test
    void testNewInstanceNoNamespaceProp() {
        InMemoryBlueGreenStatePublisher statePublisher = new InMemoryBlueGreenStatePublisher("test-namespace");
        BlueGreenState blueGreenState = statePublisher.getBlueGreenState();
        Assertions.assertEquals("test-namespace", blueGreenState.getCurrent().getNamespace());
        Assertions.assertEquals(State.ACTIVE, blueGreenState.getCurrent().getState());
        Assertions.assertEquals(new Version("v1"), blueGreenState.getCurrent().getVersion());
    }
}
