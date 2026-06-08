package com.netcracker.cloud.podsecrets;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Expands a lowercase_underscore file name into all three canonical forms:
 * <ul>
 *   <li>{@code db_password}  — original (snake_case lowercase)</li>
 *   <li>{@code DB_PASSWORD}  — env-var style</li>
 *   <li>{@code db.password}  — dot-notation (Quarkus/Spring config style)</li>
 * </ul>
 *
 * Normalisation for lookup: upper-case → lower-case, dots → underscores.
 */
class KeyExpander {

    private KeyExpander() {
    }

    /** Returns the three canonical forms for the given lowercase file name. */
    static Set<String> expand(String fileKey) {
        String lower = fileKey.toLowerCase();
        String upper = lower.toUpperCase();
        String dotted = lower.replace('_', '.');
        Set<String> result = new LinkedHashSet<>(4);
        result.add(lower);
        result.add(upper);
        result.add(dotted);
        return result;
    }

    /**
     * Normalises an arbitrary property name to the internal lowercase_underscore form
     * used as the map key.
     */
    static String normalise(String propertyName) {
        return propertyName.toLowerCase().replace('.', '_');
    }
}
