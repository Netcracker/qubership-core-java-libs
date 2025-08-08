package com.netcracker.cloud.maas.bluegreen.versiontracker.impl;

import org.qubership.cloud.bluegreen.api.model.BlueGreenState;
import org.qubership.cloud.bluegreen.api.model.NamespaceVersion;
import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.model.Version;
import org.qubership.cloud.bluegreen.impl.service.ConsulBlueGreenStatePublisher;
import org.qubership.cloud.maas.bluegreen.versiontracker.api.TrackingVersionFilter;
import org.qubership.cloud.maas.client.impl.Env;

import java.util.function.Predicate;

import static org.qubership.cloud.maas.bluegreen.versiontracker.impl.VersionFilterConstructor.constructVersionFilter;

public class LocalDevTrackingVersionFilter implements TrackingVersionFilter {
    private BlueGreenState state;
    private Predicate<String> predicate;

    public LocalDevTrackingVersionFilter() {
        this(Env.namespace());
    }

    public LocalDevTrackingVersionFilter(String namespace) {
        this(namespace, new Version("v1"));
    }

    public LocalDevTrackingVersionFilter(String namespace, Version version) {
        this(new NamespaceVersion(namespace, State.ACTIVE, version));
    }

    public LocalDevTrackingVersionFilter(NamespaceVersion version) {
        this(new BlueGreenState(version, ConsulBlueGreenStatePublisher.UNKNOWN_DATETIME));
    }

    public LocalDevTrackingVersionFilter(BlueGreenState blueGreenState) {
        setState(blueGreenState);
    }

    @Override
    public synchronized boolean test(String version) {
        return this.predicate.test(version);
    }

    public synchronized LocalDevTrackingVersionFilter setState(BlueGreenState state) {
        this.state = state;
        this.predicate = constructVersionFilter(state);
        return this;
    }

    public synchronized BlueGreenState getState() {
        return this.state;
    }
}
