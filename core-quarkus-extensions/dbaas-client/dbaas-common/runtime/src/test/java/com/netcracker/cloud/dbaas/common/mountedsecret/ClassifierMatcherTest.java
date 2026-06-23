package com.netcracker.cloud.dbaas.common.mountedsecret;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassifierMatcherTest {

    @Test
    void matchingKeyCollapsesNullTypeAndRoleToEmptySegments() {
        Map<String, Object> c = Map.of("scope", "service", "microserviceName", "svc");
        String key = ClassifierMatcher.matchingKey(c, null, null);
        assertTrue(key.endsWith("||"), "null type and null role become empty key segments");
    }

    @Test
    void matchingKeyLowerCasesTypeAndTrimsRole() {
        Map<String, Object> c = Map.of("scope", "service", "microserviceName", "svc");
        assertEquals(ClassifierMatcher.matchingKey(c, "postgresql", "admin"),
                ClassifierMatcher.matchingKey(c, "POSTGRESQL", "  admin  "));
    }

    @Test
    void canonicalIgnoresNullValuesAndEmptyNestedObjectsAndKeepsScalars() {
        Map<String, Object> full = new LinkedHashMap<>();
        full.put("scope", "SERVICE");
        full.put("microserviceName", "svc");
        full.put("ignored", null);
        full.put("emptyNested", new HashMap<>());
        full.put("count", 3);
        full.put("flag", true);

        Map<String, Object> equivalent = new LinkedHashMap<>();
        equivalent.put("microserviceName", "svc");
        equivalent.put("scope", "service");
        equivalent.put("count", 3);
        equivalent.put("flag", true);

        assertEquals(ClassifierMatcher.canonical(equivalent), ClassifierMatcher.canonical(full),
                "null values and empty nested objects must not affect the canonical key");
    }

    @Test
    void canonicalKeepsNonEmptyNestedObjects() {
        Map<String, Object> withNested = new LinkedHashMap<>();
        withNested.put("scope", "service");
        Map<String, Object> ck = new HashMap<>();
        ck.put("logicalDbName", "x");
        withNested.put("customKeys", ck);

        Map<String, Object> withoutNested = Map.of("scope", "service");
        assertNotEquals(ClassifierMatcher.canonical(withoutNested), ClassifierMatcher.canonical(withNested),
                "a non-empty nested object is part of the canonical key");
    }
}
