package com.netcracker.routes.gateway.plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

public final class RootReducer {

    public static final long DEFAULT_FACADE_TIMEOUT_MS = 120_000L;

    private RootReducer() {
    }

    public static List<Rule> reduce(List<Rule> rules) {
        List<Rule> sorted = new ArrayList<>(rules);
        sorted.sort(Comparator.comparingInt((Rule r) -> Rules.segments(r.match()).length).thenComparing(Rule::match));

        TreeMap<String, Rule> roots = new TreeMap<>();
        for (Rule rule : sorted) {
            String[] ruleSegments = Rules.segments(rule.match());
            String candidate = null;
            int candidateLen = -1;
            for (int len = ruleSegments.length; len >= 0; len--) {
                String ancestor = Rules.join(ruleSegments, 0, len);
                if (roots.containsKey(ancestor)) {
                    candidate = ancestor;
                    candidateLen = len;
                    break;
                }
            }

            if (candidate == null) {
                roots.put(rule.match(), rule);
                continue;
            }

            Rule existing = roots.get(candidate);
            String suffix = candidateLen == ruleSegments.length ? "" : Rules.join(ruleSegments, candidateLen, ruleSegments.length);
            String computedTarget = suffix.isEmpty() ? existing.target() : Rules.normalize(existing.target() + suffix);

            if (computedTarget.equals(rule.target())) {
                Set<String> mergedSources = new HashSet<>(existing.sources());
                mergedSources.addAll(rule.sources());
                long mergedTimeout = Math.max(existing.timeout(), rule.timeout());
                roots.put(candidate, new Rule(existing.match(), existing.target(), mergedTimeout, mergedSources));
            } else if (candidate.equals(rule.match())) {
                throw new RouteGenerationException(
                        "Conflicting targets for " + rule.match() + ": " + existing.target() + " and " + rule.target());
            } else {
                roots.put(rule.match(), rule);
            }
        }

        if (roots.containsKey("/")) {
            Rule rootSlash = roots.get("/");
            throw new RouteGenerationException(
                    "Root route \"/\" is not allowed, produced by class(es): " + String.join(", ", rootSlash.sources()));
        }

        return new ArrayList<>(roots.values());
    }

    public static List<Rule> applyFacadeFilter(List<Rule> facadeRoots) {
        boolean anyNeedsRendering = facadeRoots.stream()
                .anyMatch(r -> !r.match().equals(r.target()) || r.timeout() > DEFAULT_FACADE_TIMEOUT_MS);
        return anyNeedsRendering ? facadeRoots : List.of();
    }
}
