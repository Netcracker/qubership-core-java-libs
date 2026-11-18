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
        sorted.sort(Comparator.comparingInt(RootReducer::segmentCount).thenComparing(Rule::match));

        TreeMap<String, Rule> roots = new TreeMap<>();
        for (Rule rule : sorted) {
            String[] ruleSegments = segments(rule.match());
            String candidate = null;
            for (int len = ruleSegments.length; len >= 0; len--) {
                String ancestor = join(ruleSegments, len);
                if (roots.containsKey(ancestor)) {
                    candidate = ancestor;
                    break;
                }
            }

            if (candidate == null) {
                roots.put(rule.match(), rule);
                continue;
            }

            Rule existing = roots.get(candidate);
            String suffix;
            if (candidate.equals(rule.match())) {
                suffix = "";
            } else if (candidate.equals("/")) {
                suffix = rule.match();
            } else {
                suffix = rule.match().substring(candidate.length());
            }
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

    private static int segmentCount(Rule rule) {
        return segments(rule.match()).length;
    }

    private static String[] segments(String match) {
        if (match.equals("/")) {
            return new String[0];
        }
        return match.substring(1).split("/");
    }

    private static String join(String[] segments, int count) {
        if (count == 0) {
            return "/";
        }
        return "/" + String.join("/", java.util.Arrays.asList(segments).subList(0, count));
    }
}
