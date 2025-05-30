package org.qubership.maas.declarative.kafka.client.impl;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void merge_shouldCombineTwoMaps() {
        Map<String, Object> mapA = Map.of(
            "key1", "value1",
            "key2", "value2"
        );
        Map<String, Object> mapB = Map.of(
            "key3", "value3",
            "key4", "value4"
        );

        Map<String, Object> result = Utils.merge(mapA, mapB);

        assertEquals(4, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
        assertEquals("value3", result.get("key3"));
        assertEquals("value4", result.get("key4"));
    }

    @Test
    void merge_shouldHandleNullMaps() {
        Map<String, Object> mapA = Map.of("key1", "value1");

        Map<String, Object> result1 = Utils.merge(mapA, null);
        assertEquals(1, result1.size());
        assertEquals("value1", result1.get("key1"));

        Map<String, Object> result2 = Utils.merge(null, mapA);
        assertEquals(1, result2.size());
        assertEquals("value1", result2.get("key1"));

        Map<String, Object> result3 = Utils.merge(null, null);
        assertTrue(result3.isEmpty());
    }

    @Test
    void merge_shouldOverrideValuesFromSecondMap() {
        Map<String, Object> mapA = Map.of(
            "key1", "value1",
            "key2", "value2"
        );
        Map<String, Object> mapB = Map.of(
            "key1", "newValue1",
            "key3", "value3"
        );

        Map<String, Object> result = Utils.merge(mapA, mapB);

        assertEquals(3, result.size());
        assertEquals("newValue1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
        assertEquals("value3", result.get("key3"));
    }

    @Test
    void prepareConfigAsMap_shouldConvertTypesCorrectly() {
        Map<String, Object> config = new HashMap<>();
        config.put("test.int", 42);
        config.put("test.double", 3.14);
        config.put("test.boolean", true);
        config.put("test.string", "hello");
        config.put("test.true", "true");
        config.put("test.false", "false");
        config.put("test.trim", "  spaces  ");

        Map<String, Object> result = Utils.prepareConfigAsMap(config, "test.");

        assertEquals(7, result.size());
        assertInstanceOf(Integer.class, result.get("int"));
        assertEquals(42, result.get("int"));
        assertInstanceOf(Double.class, result.get("double"));
        assertEquals(3.14, result.get("double"));
        assertInstanceOf(Boolean.class, result.get("boolean"));
        assertTrue((Boolean) result.get("boolean"));
        assertInstanceOf(String.class, result.get("string"));
        assertEquals("hello", result.get("string"));
        assertInstanceOf(Boolean.class, result.get("true"));
        assertTrue((Boolean) result.get("true"));
        assertInstanceOf(Boolean.class, result.get("false"));
        assertFalse((Boolean) result.get("false"));
        assertInstanceOf(String.class, result.get("trim"));
        assertEquals("spaces", result.get("trim"));
    }

    @Test
    void prepareConfigAsMap_shouldHandleEmptyConfig() {
        Map<String, Object> result = Utils.prepareConfigAsMap(new HashMap<>(), "test.");
        assertTrue(result.isEmpty());
    }

    @Test
    void prepareConfigAsMap_shouldHandleNonMatchingPrefix() {
        Map<String, Object> config = Map.of("other.key", "value");
        Map<String, Object> result = Utils.prepareConfigAsMap(config, "test.");
        assertTrue(result.isEmpty());
    }

    @Test
    void safe_shouldExecuteRunnable() {
        boolean[] executed = {false};
        Utils.safe(() -> executed[0] = true);
        assertTrue(executed[0]);
    }

    @Test
    void safe_shouldSuppressException() {
        Utils.safe(() -> {
            throw new RuntimeException("Test exception");
        });
        // Test passes if no exception is thrown
    }
}
