package com.netcracker.cloud.bluegreen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.bluegreen.api.model.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class VersionTest {
    @Test
    void testDeserialization() throws JsonProcessingException {
        Version result = new ObjectMapper().readValue("2", Version.class);
        Assertions.assertEquals("v2", result.value());
    }

    @Test
    void testConstructor() {
        var version = new Version("v3");
        assertEquals("v3", version.value());
        assertEquals(3, version.intValue());
    }

    @Test
    void testNormalizationFailure() {
        assertThrows(IllegalArgumentException.class, () -> new Version("abc"));
    }

    @Test
    void testToString() {
        assertEquals("v2", new Version("2").toString());
    }

    @Test
    void testEquals() {
        assertEquals(new Version("2"), new Version("v2"));
        assertEquals(new Version("v2"), new Version("2"));
    }

    @Test
    void testEquals_withString() {
        assertEquals(new Version("v2"), new Version("2"));
    }

    @Test
    void testEqualsFalse() {
        assertNotEquals(new Version("v3"), new Version("2"));
    }

    @Test
    void testEquals_incompatibleType() {
        assertFalse(new Version("v3").equals(3));
    }

    @Test
    void testEquals_withNull() {
        assertFalse(new Version("2").equals(null));
    }

    @Test
    void testEmpty() {
        Version test1 = new Version("");
        assertEquals(0, test1.intValue());
        assertEquals("", test1.value());
        Version test2 = new Version(null);
        assertEquals(0, test2.intValue());
        assertEquals("", test2.value());
    }

    @Test
    void testComparable() {
        var v1 = new Version("v1");
        var v2 = new Version("v2");
        var v3 = new Version("v3");

        assertTrue(v1.compareTo(v2) < 0);
        assertTrue(v2.compareTo(v1) > 0);
        assertTrue(v3.compareTo(v2) > 0);
        assertTrue(v3.compareTo(v1) > 0);
        assertEquals(0, v1.compareTo(new Version("v1")));
    }

    @Test
    void testHashMap() {
        Map<Version, Integer> map = new HashMap<>();
        IntStream.range(1, 1000).boxed().forEach(v -> {
            Version version = new Version(v);
            map.put(version, v);
            assertEquals(v, map.get(version));
        });
    }
}
