package com.netcracker.cloud.dbaas.client.service.mountedsecret;

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
        full.put("scope", "SERVICE");                 // lower-cased
        full.put("microserviceName", "svc");
        full.put("ignored", null);                    // null value -> omitted
        full.put("emptyNested", new HashMap<>());     // empty nested object -> dropped
        full.put("count", 3);                         // number kept as-is
        full.put("flag", true);                       // boolean kept as-is

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

    @Test
    void matchingKeyIsCaseSensitiveForRole() {
        Map<String, Object> c = Map.of("scope", "service", "microserviceName", "svc");
        assertNotEquals(ClassifierMatcher.matchingKey(c, "postgresql", "admin"),
                ClassifierMatcher.matchingKey(c, "postgresql", "Admin"),
                "role is matched case-sensitively (unlike type, which is lower-cased)");
    }

    @Test
    void canonicalDiffersWhenTopLevelKeySetDiffers() {
        Map<String, Object> base = Map.of("scope", "service", "microserviceName", "svc", "namespace", "team-a");
        Map<String, Object> withExtra = new LinkedHashMap<>(base);
        withExtra.put("logicalDbName", "reports");
        assertNotEquals(ClassifierMatcher.canonical(base), ClassifierMatcher.canonical(withExtra),
                "an arbitrary top-level identity key present on only one side must diverge the canonical key");
    }

    @Test
    void canonicalDiffersForServiceVsTenantScope() {
        Map<String, Object> service = Map.of("scope", "service", "microserviceName", "svc", "namespace", "team-a");
        Map<String, Object> tenant = Map.of("scope", "tenant", "microserviceName", "svc", "namespace", "team-a", "tenantId", "acme");
        assertNotEquals(ClassifierMatcher.canonical(service), ClassifierMatcher.canonical(tenant),
                "service and tenant scope are different identities");
    }

    @Test
    void canonicalDiffersForDifferentTenantId() {
        Map<String, Object> acme = Map.of("scope", "tenant", "microserviceName", "svc", "namespace", "team-a", "tenantId", "acme");
        Map<String, Object> globex = Map.of("scope", "tenant", "microserviceName", "svc", "namespace", "team-a", "tenantId", "globex");
        assertNotEquals(ClassifierMatcher.canonical(acme), ClassifierMatcher.canonical(globex),
                "different tenantId is a different identity");
    }
}
