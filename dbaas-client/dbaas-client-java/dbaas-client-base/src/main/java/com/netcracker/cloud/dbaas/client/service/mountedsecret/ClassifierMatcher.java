package com.netcracker.cloud.dbaas.client.service.mountedsecret;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Builds the canonical lookup key used to match a request's (classifier, type, role) against a
 * mounted Secret's descriptor. Both the client's own classifier and the descriptor's classifier are
 * canonicalized by this same code, so they only have to agree with each other — there is no need to
 * be byte-identical with any other language client.
 * <p>
 * Canonicalization mirrors the Go provider semantics: keys sorted recursively, {@code scope}
 * lower-cased, empty top-level {@code namespace}/{@code tenantId} omitted, and empty nested objects
 * dropped. Role matching is exact-string after trim (case-sensitive), and an empty role matches a
 * descriptor whose {@code userRole} was left unset.
 */
@Slf4j
final class ClassifierMatcher {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Object OMIT = new Object();

    private ClassifierMatcher() {
    }

    static String matchingKey(Map<String, Object> classifier, String type, String role) {
        return canonical(classifier)
                + "|" + (type == null ? "" : type.toLowerCase(Locale.ROOT))
                + "|" + (role == null ? "" : role.strip());
    }

    static String canonical(Map<String, Object> classifier) {
        try {
            return MAPPER.writeValueAsString(normalizeMap(classifier, true));
        } catch (Exception e) {
            log.warn("mounted-secret: failed to canonicalize classifier {}: {}", classifier, e.toString());
            return "";
        }
    }

    private static Object normalizeMap(Map<String, Object> map, boolean topLevel) {
        TreeMap<String, Object> out = new TreeMap<>();
        if (map != null) {
            for (Map.Entry<String, Object> e : map.entrySet()) {
                Object normalized = normalizeValue(e.getKey(), e.getValue(), topLevel);
                if (normalized != OMIT) {
                    out.put(e.getKey(), normalized);
                }
            }
        }
        // Drop empty nested objects; keep the (possibly empty) top-level object.
        if (!topLevel && out.isEmpty()) {
            return null;
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Object normalizeValue(String key, Object value, boolean topLevel) {
        if (value == null) {
            return OMIT;
        }
        if (value instanceof String s) {
            if ("scope".equals(key)) {
                s = s.toLowerCase(Locale.ROOT);
            }
            if (topLevel && s.isEmpty() && ("namespace".equals(key) || "tenantId".equals(key))) {
                return OMIT;
            }
            return s;
        }
        if (value instanceof Map<?, ?> nested) {
            Object normalizedNested = normalizeMap((Map<String, Object>) nested, false);
            return normalizedNested == null ? OMIT : normalizedNested;
        }
        // numbers / booleans / arrays are serialized as-is (no recursion into arrays, matching Go).
        return value;
    }
}
