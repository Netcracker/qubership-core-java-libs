package org.qubership.maas.declarative.kafka.client.impl;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsTest {

    @Test
    void merge() {
        assertEquals(Map.of(
                        "k1", "v1",
                        "k2", "v2"),
                Utils.merge(Map.of("k1", "v1"), Map.of("k2", "v2")));
        assertEquals(Map.of(
                        "k1", "v1",
                        "k2", "v2"),
                Utils.merge(Map.of("k1", "v1", "k2", "v2"), Map.of("k2", "v2")));
        assertEquals(Map.of(), Utils.merge(Map.of(), Map.of()));
        assertEquals(Map.of(), Utils.merge(null, Map.of()));
        assertEquals(Map.of(), Utils.merge(Map.of(), null));
        assertEquals(Map.of(), Utils.merge(null, null));

    }
}