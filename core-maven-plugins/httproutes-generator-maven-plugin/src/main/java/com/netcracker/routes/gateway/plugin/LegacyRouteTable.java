package com.netcracker.routes.gateway.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * How the legacy Cloud-Core Service Mesh routed this service's border routes, one table per border gateway.
 * <p>
 * Reproduces legacy registration ({@code RouteTransformer}): every border route is registered on all three
 * border gateways, allowed on the gateways its type exposes and forbidden on the wider ones, and an allowed entry
 * wins over a forbidden one with the same gateway path. {@code @ForbiddenRoute} declarations add forbidden entries.
 * Facade and composite routes are not part of the model.
 */
public final class LegacyRouteTable {

    private static final Comparator<LegacyEntry> ENTRY_ORDER = Comparator
            .comparingInt((LegacyEntry e) -> -e.pattern().length())
            .thenComparing(e -> e.pattern().source())
            .thenComparing(LegacyEntry::kind)
            .thenComparing(LegacyEntry::origin);

    private final Map<Gateway, List<LegacyEntry>> entries;
    private final Map<Gateway, Set<PathPattern>> legacyInvalidPatterns;
    private final List<Finding> findings;

    private LegacyRouteTable(Map<Gateway, List<LegacyEntry>> entries,
                             Map<Gateway, Set<PathPattern>> legacyInvalidPatterns,
                             List<Finding> findings) {
        this.entries = entries;
        this.legacyInvalidPatterns = legacyInvalidPatterns;
        this.findings = List.copyOf(findings);
    }

    public static LegacyRouteTable build(RouteDeclarations declarations) {
        Map<Gateway, List<LegacyEntry>> entries = new EnumMap<>(Gateway.class);
        Map<Gateway, Set<PathPattern>> invalid = new EnumMap<>(Gateway.class);
        List<Finding> findings = new ArrayList<>();
        for (Gateway gateway : Gateway.values()) {
            List<LegacyEntry> table = new ArrayList<>();
            Map<PathPattern, List<DeclaredRoute>> allowed = new LinkedHashMap<>();
            List<DeclaredRoute> narrower = new ArrayList<>();
            for (DeclaredRoute route : declarations.routes()) {
                HttpRoute.Type type = route.route().type();
                if (type == HttpRoute.Type.FACADE) {
                    continue;
                }
                if (Gateway.exposedBy(type).contains(gateway)) {
                    allowed.computeIfAbsent(PathPattern.of(route.route().gatewayPath()), p -> new ArrayList<>()).add(route);
                    table.add(LegacyEntry.allowed(route));
                } else {
                    narrower.add(route);
                }
            }
            for (DeclaredRoute route : narrower) {
                if (!allowed.containsKey(PathPattern.of(route.route().gatewayPath()))) {
                    table.add(LegacyEntry.implicitForbidden(route));
                }
            }
            for (ForbiddenDeclaration declaration : declarations.forbidden()) {
                if (!declaration.gateways().contains(gateway)) {
                    continue;
                }
                List<DeclaredRoute> contradicted = allowed.get(PathPattern.of(declaration.gatewayPath()));
                if (contradicted == null) {
                    table.add(LegacyEntry.explicitForbidden(declaration));
                } else {
                    findings.add(contradictoryDeclaration(gateway, declaration, contradicted));
                }
            }
            Set<PathPattern> invalidPatterns = new TreeSet<>(Comparator.comparing(PathPattern::source));
            allowed.forEach((pattern, routes) -> {
                Set<String> servicePaths = routes.stream().map(r -> r.route().path()).collect(Collectors.toSet());
                if (servicePaths.size() > 1) {
                    invalidPatterns.add(pattern);
                    findings.add(duplicateGatewayPath(gateway, pattern, routes));
                }
            });
            table.sort(ENTRY_ORDER);
            entries.put(gateway, List.copyOf(table));
            invalid.put(gateway, Collections.unmodifiableSet(invalidPatterns));
        }
        return new LegacyRouteTable(entries, invalid, findings);
    }

    /**
     * @return the entries of the gateway, longest pattern first
     */
    public List<LegacyEntry> entries(Gateway gateway) {
        return entries.get(gateway);
    }

    /**
     * @return gateway paths with two allowed routes to different service paths, reported as {@code LEGACY_INVALID}
     */
    public Set<PathPattern> legacyInvalidPatterns(Gateway gateway) {
        return legacyInvalidPatterns.get(gateway);
    }

    /**
     * @return contradictory declarations and legacy-invalid duplicate routes
     */
    public List<Finding> findings() {
        return findings;
    }

    /**
     * Legacy decision: the matching entry with the longest gateway path wins. Entries of equal length decide together
     * only if they are all forbidden, or all allowed with the same upstream path (then with the largest timeout).
     */
    public Decision decide(Gateway gateway, String requestPath) {
        List<LegacyEntry> tied = new ArrayList<>();
        List<PathPattern.Match> matches = new ArrayList<>();
        int longest = -1;
        for (LegacyEntry entry : entries.get(gateway)) {
            if (entry.pattern().length() < longest) {
                break;
            }
            Optional<PathPattern.Match> match = entry.pattern().match(requestPath);
            if (match.isPresent()) {
                longest = entry.pattern().length();
                tied.add(entry);
                matches.add(match.get());
            }
        }
        if (tied.isEmpty()) {
            return new Decision.Unrouted();
        }
        if (tied.stream().noneMatch(LegacyEntry::isAllowed)) {
            return new Decision.Forbidden(tied);
        }
        if (tied.stream().allMatch(LegacyEntry::isAllowed)) {
            Set<String> upstreams = new TreeSet<>();
            for (int i = 0; i < tied.size(); i++) {
                upstreams.add(tied.get(i).upstream(matches.get(i)));
            }
            if (upstreams.size() == 1) {
                long timeout = tied.stream().mapToLong(LegacyEntry::timeout).max().orElse(0);
                return new Decision.Routed(upstreams.iterator().next(), timeout, tied);
            }
        }
        return new Decision.Undefined(tied);
    }

    private static Finding contradictoryDeclaration(Gateway gateway, ForbiddenDeclaration declaration,
                                                    List<DeclaredRoute> routes) {
        return Finding.of(Finding.Kind.CONTRADICTORY_DECLARATION, gateway.refName(),
                "element", declaration.origin(),
                "path", declaration.gatewayPath(),
                "route", routes.stream().map(DeclaredRoute::describe).collect(Collectors.joining("; ")),
                "problem", "@ForbiddenRoute forbids the gateway path of a route that is allowed on " + gateway.refName(),
                "fix", "remove RouteType." + gateway.routeType() + " from @ForbiddenRoute on " + declaration.origin()
                        + ", or change the route type so that the route is not allowed on " + gateway.refName());
    }

    private static Finding duplicateGatewayPath(Gateway gateway, PathPattern pattern, List<DeclaredRoute> routes) {
        List<String> details = new ArrayList<>(List.of("path", pattern.source()));
        routes.stream()
                .sorted(Comparator.comparing((DeclaredRoute r) -> r.route().path()).thenComparing(DeclaredRoute::originsText))
                .forEach(route -> {
                    details.add("route");
                    details.add(route.describe());
                });
        details.addAll(List.of(
                "problem", "the routes are allowed on " + gateway.refName() + " with the same gateway path and different "
                        + "service paths; the legacy runtime rejects this too (\"several target paths for forwarding "
                        + "from the same source path\")",
                "fix", "use one service path for gateway path " + pattern.source() + ", or change the gateway path of "
                        + "one of the routes"));
        return Finding.of(Finding.Kind.LEGACY_INVALID, gateway.refName(), details.toArray(String[]::new));
    }

    /**
     * Legacy decision for one request path on one gateway. There is no backend: routed requests go to this service.
     */
    public sealed interface Decision {

        /**
         * @param entries the winning entries, several when entries of equal length agree
         */
        record Routed(String upstreamPath, long timeout, List<LegacyEntry> entries) implements Decision {
            public Routed {
                entries = List.copyOf(entries);
            }
        }

        record Forbidden(List<LegacyEntry> entries) implements Decision {
            public Forbidden {
                entries = List.copyOf(entries);
            }
        }

        record Unrouted() implements Decision {
        }

        /**
         * Entries of equal length that disagree: legacy picked one of them in an undefined order.
         */
        record Undefined(List<LegacyEntry> tied) implements Decision {
            public Undefined {
                tied = List.copyOf(tied);
            }
        }
    }
}
