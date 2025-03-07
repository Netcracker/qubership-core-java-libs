package org.qubership.cloud.bluegreen.impl.service;

import org.qubership.cloud.bluegreen.api.model.BlueGreenState;
import org.qubership.cloud.bluegreen.api.model.NamespaceVersion;
import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.model.Version;
import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.impl.util.EnvUtil;

import java.util.*;
import java.util.function.Consumer;

public class InMemoryBlueGreenStatePublisher implements BlueGreenStatePublisher {

    BlueGreenState state;
    Set<Consumer<BlueGreenState>> subscribers = Collections.synchronizedSet(new HashSet<>());

    public InMemoryBlueGreenStatePublisher() {
        this(EnvUtil.getNamespace());
    }

    public InMemoryBlueGreenStatePublisher(String namespace) {
        this(namespace, new Version("v1"));
    }

    public InMemoryBlueGreenStatePublisher(String namespace, Version version) {
        this(new NamespaceVersion(namespace, State.ACTIVE, version));
    }

    public InMemoryBlueGreenStatePublisher(NamespaceVersion version) {
        this(new BlueGreenState(version, ConsulBlueGreenStatePublisher.UNKNOWN_DATETIME));
    }

    public InMemoryBlueGreenStatePublisher(BlueGreenState state) {
        setBlueGreenState(state);
    }

    @Override
    public synchronized void subscribe(Consumer<BlueGreenState> subscriber) {
        subscribers.add(subscriber);
        notifySubscribers(List.of(subscriber));
    }

    @Override
    public synchronized void unsubscribe(Consumer<BlueGreenState> subscriber) {
        subscribers.remove(subscriber);
    }

    @Override
    public synchronized BlueGreenState getBlueGreenState() {
        return this.state;
    }

    public synchronized void setBlueGreenState(BlueGreenState state) {
        this.state = state;
        notifySubscribers(this.subscribers);
    }

    private void notifySubscribers(Collection<Consumer<BlueGreenState>> subscribers) {
        subscribers.forEach(s -> s.accept(state));
    }
}
