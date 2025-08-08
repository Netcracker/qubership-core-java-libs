package com.netcracker.cloud.maas.bluegreen.versiontracker.impl;

import com.netcracker.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import com.netcracker.cloud.bluegreen.impl.service.ConsulBlueGreenStatePublisher;
import com.netcracker.cloud.maas.bluegreen.versiontracker.api.TrackingVersionFilter;
import lombok.SneakyThrows;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.netcracker.cloud.maas.bluegreen.versiontracker.impl.VersionFilterConstructor.constructVersionFilter;

public class TrackingVersionFilterImpl implements TrackingVersionFilter, AutoCloseable {
    private final BlueGreenStatePublisher statePublisher;
    private final AtomicReference<Predicate<String>> predicate;

    public TrackingVersionFilterImpl(Supplier<String> consultTokenSupplier) {
        this(new ConsulBlueGreenStatePublisher(consultTokenSupplier));
    }

    public TrackingVersionFilterImpl(BlueGreenStatePublisher publisher) {
        statePublisher = publisher;
        predicate = new AtomicReference<>(constructVersionFilter(statePublisher.getBlueGreenState()));
        statePublisher.subscribe(state -> predicate.set(constructVersionFilter(state)));
    }


    @Override
    public boolean test(String version) {
        return predicate.get().test(version);
    }

    @Override
    @SneakyThrows
    public void close() {
        if (statePublisher instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }
}
