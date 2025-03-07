package org.qubership.cloud.bluegreen.impl.service;

import org.qubership.cloud.bluegreen.api.model.BlueGreenState;
import org.qubership.cloud.bluegreen.api.model.NamespaceVersion;
import org.qubership.cloud.bluegreen.api.model.Version;

/**
 * Use {@link InMemoryBlueGreenStatePublisher}
 */
@Deprecated(since = "0.2.3")
public class LocalDevBlueGreenStatePublisher extends InMemoryBlueGreenStatePublisher {
    public LocalDevBlueGreenStatePublisher() {
    }

    public LocalDevBlueGreenStatePublisher(String namespace) {
        super(namespace);
    }

    public LocalDevBlueGreenStatePublisher(String namespace, Version version) {
        super(namespace, version);
    }

    public LocalDevBlueGreenStatePublisher(NamespaceVersion version) {
        super(version);
    }

    public LocalDevBlueGreenStatePublisher(BlueGreenState state) {
        super(state);
    }
}
