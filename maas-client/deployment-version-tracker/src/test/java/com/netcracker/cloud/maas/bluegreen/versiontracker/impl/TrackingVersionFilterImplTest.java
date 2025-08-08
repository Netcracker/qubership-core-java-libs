package com.netcracker.cloud.maas.bluegreen.versiontracker.impl;

import org.qubership.cloud.bluegreen.api.model.BlueGreenState;
import org.qubership.cloud.bluegreen.api.model.NamespaceVersion;
import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.model.Version;
import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class TrackingVersionFilterImplTest {

    @Test
    void testFilter() {
        ClosableBlueGreenStatePublisher closablePublisher = Mockito.mock(ClosableBlueGreenStatePublisher.class);
        BlueGreenState blueGreenState1 = new BlueGreenState(
                new NamespaceVersion("namespace-1", State.ACTIVE, new Version(1)),
                new NamespaceVersion("namespace-2", State.CANDIDATE, new Version(2)),
                OffsetDateTime.now());
        BlueGreenState blueGreenState2 = new BlueGreenState(
                new NamespaceVersion("namespace-1", State.LEGACY, new Version(1)),
                new NamespaceVersion("namespace-2", State.ACTIVE, new Version(2)),
                OffsetDateTime.now());
        Mockito.when(closablePublisher.getBlueGreenState()).thenReturn(blueGreenState1);
        AtomicReference<Consumer<BlueGreenState>> consumer = new AtomicReference<>();
        Mockito.doAnswer(i -> {
            consumer.set(i.getArgument(0, Consumer.class));
            consumer.get().accept(blueGreenState1);
            return null;
        }).when(closablePublisher).subscribe(Mockito.any());
        try (TrackingVersionFilterImpl filter = new TrackingVersionFilterImpl(closablePublisher)) {
            Assertions.assertTrue(filter.test("v1"));
            Assertions.assertTrue(filter.test(""));
            Assertions.assertFalse(filter.test("v2"));

            consumer.get().accept(blueGreenState2);

            Assertions.assertTrue(filter.test("v1"));
            Assertions.assertFalse(filter.test(""));
            Assertions.assertFalse(filter.test("v2"));
        }
        Mockito.verify(closablePublisher).close();
    }

    interface ClosableBlueGreenStatePublisher extends BlueGreenStatePublisher, AutoCloseable {
        @Override
        void close();
    }
}
