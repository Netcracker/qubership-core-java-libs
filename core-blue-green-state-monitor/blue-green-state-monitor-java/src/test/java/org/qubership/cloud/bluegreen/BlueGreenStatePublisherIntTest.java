package org.qubership.cloud.bluegreen;

import org.qubership.cloud.bluegreen.api.model.BlueGreenState;
import org.qubership.cloud.bluegreen.api.model.NamespaceVersion;
import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.model.Version;
import org.qubership.cloud.bluegreen.impl.dto.BGState;
import org.qubership.cloud.bluegreen.impl.dto.NSVersion;
import org.qubership.cloud.bluegreen.impl.http.ObjectMapperPublisher;
import org.qubership.cloud.bluegreen.impl.service.ConsulBlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.impl.util.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static org.qubership.cloud.bluegreen.impl.service.ConsulBlueGreenStatePublisher.UNKNOWN_DATETIME;


class BlueGreenStatePublisherIntTest extends AbstractBGTest {

    @Test
    void testJson() {
        String stateJson = String.format("""
                  {
                    "updateTime": "2023-07-07T10:00:54Z",
                    "originNamespace": {
                        "name": "%s",
                        "state": "active",
                        "version": "v2"
                    },
                    "peerNamespace": {
                        "name": "%s",
                        "state": "idle",
                        "version": null
                    }
                  }
                """, ns1, ns2);
        saveBGState(namespaces, stateJson);

        try (ConsulBlueGreenStatePublisher stateListener = new ConsulBlueGreenStatePublisher(consulTokenSupplier, consulUrl, ns1)) {
            BlueGreenState bgState = stateListener.getBlueGreenState();
            Assertions.assertEquals(OffsetDateTime.parse("2023-07-07T10:00:54Z"), bgState.getUpdateTime());

            NamespaceVersion current = bgState.getCurrent();
            Assertions.assertEquals(ns1, current.getNamespace());
            Assertions.assertEquals(State.ACTIVE, current.getState());
            Assertions.assertEquals(new Version("v2"), current.getVersion());

            Assertions.assertTrue(bgState.getSibling().isPresent());
            NamespaceVersion sibling = bgState.getSibling().get();
            Assertions.assertEquals(ns2, sibling.getNamespace());
            Assertions.assertEquals(State.IDLE, sibling.getState());
            Assertions.assertEquals(new Version(""), sibling.getVersion());
        }
    }

    @Test
    void testStateChange() {
        OffsetDateTime updateTimeState1 = OffsetDateTime.parse("2023-07-25T01:00:00Z");
        OffsetDateTime updateTimeState2 = OffsetDateTime.parse("2023-07-25T03:00:00Z");

        BGState state1 = new BGState(
                new NSVersion(ns1, State.ACTIVE.getName(), "v1"),
                new NSVersion(ns2, State.CANDIDATE.getName(), "v2"),
                updateTimeState1);
        saveBGState(namespaces, state1);

        Map<String, CountDownLatch> tests = Map.of(
                ns1, new CountDownLatch(2),
                ns2, new CountDownLatch(2)
        );
        List<ConsulBlueGreenStatePublisher> stateListeners = tests.entrySet().stream().map(test -> {
            String namespace = test.getKey();
            ConsulBlueGreenStatePublisher stateListener = new ConsulBlueGreenStatePublisher(consulTokenSupplier, consulUrl, namespace);
            CountDownLatch latch = test.getValue();
            AtomicInteger stateCount = new AtomicInteger(0);
            stateListener.subscribe(state -> {
                int i = stateCount.incrementAndGet();
                if (i == 1) {
                    BlueGreenState expectedState;
                    if (ns1.equals(namespace)) {
                        expectedState = new BlueGreenState(
                                new NamespaceVersion(ns1, State.ACTIVE, new Version(1)),
                                new NamespaceVersion(ns2, State.CANDIDATE, new Version(2)),
                                updateTimeState1);
                    } else {
                        expectedState = new BlueGreenState(
                                new NamespaceVersion(ns2, State.CANDIDATE, new Version(2)),
                                new NamespaceVersion(ns1, State.ACTIVE, new Version(1)),
                                updateTimeState1);
                    }
                    Assertions.assertEquals(expectedState, state);
                    latch.countDown();
                } else if (i == 2) {
                    BlueGreenState expectedState;
                    if (ns1.equals(namespace)) {
                        expectedState = new BlueGreenState(
                                new NamespaceVersion(ns1, State.LEGACY, new Version(1)),
                                new NamespaceVersion(ns2, State.ACTIVE, new Version(2)),
                                updateTimeState2);
                    } else {
                        expectedState = new BlueGreenState(
                                new NamespaceVersion(ns2, State.ACTIVE, new Version(2)),
                                new NamespaceVersion(ns1, State.LEGACY, new Version(1)), updateTimeState2);
                    }
                    Assertions.assertEquals(expectedState, state);
                    latch.countDown();
                }
            });
            return stateListener;
        }).toList();
        try {
            BGState state2 = new BGState(
                    new NSVersion(ns1, State.LEGACY.getName(), "v1"),
                    new NSVersion(ns2, State.ACTIVE.getName(), "v2"),
                    updateTimeState2);
            Executors.newSingleThreadExecutor().submit(() -> saveBGState(namespaces, state2));
            Assertions.assertTrue(tests.entrySet().stream().allMatch(e -> run(() -> e.getValue().await(10, TimeUnit.SECONDS))));
        } finally {
            // have to manually close each listener to stop their inner thread loops
            stateListeners.forEach(l -> run(l::close));
        }
    }

    @Test
    void testGetDefaultBGState() {
        try (ConsulBlueGreenStatePublisher stateListener = new ConsulBlueGreenStatePublisher(consulTokenSupplier, consulUrl, ns1)) {
            BlueGreenState bgState = stateListener.getBlueGreenState();
            BlueGreenState expectedBgState = new BlueGreenState(new NamespaceVersion(ns1, State.ACTIVE, new Version(1)), UNKNOWN_DATETIME);
            BiFunction<NamespaceVersion, NamespaceVersion, Boolean> nsEqualsFunc = (ns1, ns2) ->
                    Objects.equals(ns1.getNamespace(), ns2.getNamespace()) &&
                            Objects.equals(ns1.getState(), ns2.getState()) &&
                            Objects.equals(ns1.getVersion(), ns2.getVersion());
            Assertions.assertTrue(nsEqualsFunc.apply(expectedBgState.getCurrent(), bgState.getCurrent()));
            Assertions.assertEquals(expectedBgState.getUpdateTime(), bgState.getUpdateTime());
            Assertions.assertEquals(expectedBgState.getSibling(), bgState.getSibling());
        }
    }

    @Test
    void testSubscribeUnsubscribe() throws InterruptedException {
        try (ConsulBlueGreenStatePublisher stateListener = new ConsulBlueGreenStatePublisher(consulTokenSupplier, consulUrl, ns1)) {
            BGState state1 = new BGState(
                    new NSVersion(ns1, State.ACTIVE.getName(), "v1"),
                    new NSVersion(ns2, State.CANDIDATE.getName(), "v2"),
                    OffsetDateTime.now());

            saveBGState(namespaces, state1);

            CountDownLatch latch1 = new CountDownLatch(1);
            CountDownLatch latch2 = new CountDownLatch(1);

            Consumer<BlueGreenState> callback = s -> {
                if (Objects.equals(s.getCurrent().getVersion(), new Version(1))) {
                    latch1.countDown();
                } else if (Objects.equals(s.getCurrent().getVersion(), new Version(3))) {
                    latch2.countDown();
                }
            };
            stateListener.subscribe(callback);
            Assertions.assertTrue(latch1.await(10, TimeUnit.SECONDS));

            stateListener.unsubscribe(callback);

            BGState state2 = new BGState(
                    new NSVersion(ns1, State.ACTIVE.getName(), "v3"),
                    new NSVersion(ns2, State.CANDIDATE.getName(), "v4"),
                    OffsetDateTime.now());

            saveBGState(namespaces, state2);

            Assertions.assertFalse(latch2.await(5, TimeUnit.SECONDS));
            stateListener.subscribe(callback);

            Assertions.assertTrue(latch2.await(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void testConsulBlueGreenStatePublisherAutoClose() {
        try (ConsulBlueGreenStatePublisher blueGreenStatePublisher = new ConsulBlueGreenStatePublisher(consulTokenSupplier, consulUrl, ns1)) {
            blueGreenStatePublisher.getBlueGreenState();
            Assertions.assertEquals(1, getConsulStatePublisherThreadsCount());
        }
        retry(Duration.ofSeconds(10), () -> Assertions.assertEquals(0, getConsulStatePublisherThreadsCount()));
    }

    private void saveBGState(List<String> namespaces, Object state) {
        saveBGState(namespaces, new ObjectMapperPublisher(state));
    }

    private void saveBGState(List<String> namespaces, String state) {
        HashMap jsonMap = JsonUtil.fromJson(state, HashMap.class);
        saveBGState(namespaces, jsonMap);
    }

    private void saveBGState(List<String> namespaces, HttpRequest.BodyPublisher bodyPublisher) {
        namespaces.forEach(namespace -> this.client.invoke(req -> req.uri(URI.create(consulUrl + "/v1/kv/" +
                                String.format(ConsulBlueGreenStatePublisher.BG_STATE_CONSUL_PATH, namespace)))
                        .header("Content-Type", "application/json")
                        .PUT(bodyPublisher),
                String.class).sendAndGet());
    }

    private static long getConsulStatePublisherThreadsCount() {
        return getThreadsCount("consul-bluegreen-state-publisher");
    }
}
