package com.netcracker.routes.gateway.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Compares the legacy and Istio routing models on every border gateway for the probe request paths (design D3–D5, D11).
 */
public final class MigrationValidator {

    private static final String NOT_ROUTED = "NOT ROUTED (404)";

    private final boolean autoGenerateAuthorizationPolicies;

    public MigrationValidator(boolean autoGenerateAuthorizationPolicies) {
        this.autoGenerateAuthorizationPolicies = autoGenerateAuthorizationPolicies;
    }

    public List<Finding> validate(RouteDeclarations declarations, LegacyRouteTable legacy, IstioPlan plan) {
        IstioRouteTable istio = IstioRouteTable.of(plan);
        SortedSet<String> probes = Probes.probes(declarations, plan);
        Map<List<Object>, Finding> findings = new LinkedHashMap<>();
        for (Gateway gateway : Gateway.values()) {
            for (String probe : probes) {
                Check check = new Check(gateway, probe, legacy.decide(gateway, probe), istio.decide(gateway, probe),
                        declarations, legacy);
                check.classify(findings);
            }
        }
        return List.copyOf(findings.values());
    }

    private final class Check {
        private final Gateway gateway;
        private final String probe;
        private final LegacyRouteTable.Decision legacyDecision;
        private final IstioRouteTable.Decision istioDecision;
        private final RouteDeclarations declarations;
        private final LegacyRouteTable legacy;

        Check(Gateway gateway, String probe, LegacyRouteTable.Decision legacyDecision, IstioRouteTable.Decision istioDecision,
              RouteDeclarations declarations, LegacyRouteTable legacy) {
            this.gateway = gateway;
            this.probe = probe;
            this.legacyDecision = legacyDecision;
            this.istioDecision = istioDecision;
            this.declarations = declarations;
            this.legacy = legacy;
        }

        void classify(Map<List<Object>, Finding> findings) {
            if (legacyDecision instanceof LegacyRouteTable.Decision.Undefined undefined) {
                Set<String> patterns = undefined.tied().stream().map(e -> e.pattern().source())
                        .collect(Collectors.toCollection(TreeSet::new));
                findings.putIfAbsent(List.of(Finding.Kind.LEGACY_PRECEDENCE_UNDEFINED, gateway, patterns),
                        undefined(undefined).about(patterns));
                return;
            }
            if (istioDecision instanceof IstioRouteTable.Decision.Conflicted) {
                // reported by the planner
                return;
            }
            boolean legacyRouted = legacyDecision instanceof LegacyRouteTable.Decision.Routed;
            if (!legacyRouted && istioDecision instanceof IstioRouteTable.Decision.Routed routed) {
                add(findings, Finding.Kind.EXPOSURE, routed.rule(), exposure(routed.rule()));
            } else if (legacyDecision instanceof LegacyRouteTable.Decision.Routed legacyRoute) {
                if (istioDecision instanceof IstioRouteTable.Decision.Routed routed) {
                    if (!legacyRoute.upstreamPath().equals(routed.upstreamPath())) {
                        add(findings, Finding.Kind.CONFLICT, routed.rule(), conflict(legacyRoute, routed));
                    } else if (legacyRoute.timeout() != routed.timeout()) {
                        add(findings, Finding.Kind.TIMEOUT_CHANGE, routed.rule(), timeoutChange(legacyRoute, routed));
                    }
                } else {
                    Object rule = istioDecision instanceof IstioRouteTable.Decision.Denied denied ? denied.rule() : NOT_ROUTED;
                    add(findings, Finding.Kind.LOST_ROUTE, rule, lostRoute());
                }
            }
        }

        private void add(Map<List<Object>, Finding> findings, Finding.Kind kind, Object istioRule, List<String> details) {
            List<Object> key = List.of(kind, gateway, legacyEntries(), istioRule);
            findings.computeIfAbsent(key, k -> {
                Finding finding = Finding.of(kind, gateway.refName(), details.toArray(String[]::new));
                return kind == Finding.Kind.CONFLICT ? finding.about(collidingGatewayPaths(istioRule)) : finding;
            });
        }

        /**
         * @return the gateway paths of the legacy entries and of the sources of the Istio rule
         */
        private Set<String> collidingGatewayPaths(Object istioRule) {
            Set<String> paths = new TreeSet<>();
            legacyEntries().forEach(e -> paths.add(e.pattern().source()));
            if (istioRule instanceof PlannedRule rule) {
                rule.sources().forEach(r -> paths.add(r.route().gatewayPath()));
            }
            return paths;
        }

        private List<LegacyEntry> legacyEntries() {
            return switch (legacyDecision) {
                case LegacyRouteTable.Decision.Routed routed -> routed.entries();
                case LegacyRouteTable.Decision.Forbidden forbidden -> forbidden.entries();
                case LegacyRouteTable.Decision.Undefined undefined -> undefined.tied();
                case LegacyRouteTable.Decision.Unrouted unrouted -> List.of();
            };
        }

        private List<String> header() {
            List<String> details = new ArrayList<>(List.of("request", probe));
            List<LegacyEntry> entries = legacyEntries();
            if (entries.isEmpty()) {
                details.addAll(List.of("legacy", NOT_ROUTED));
            }
            for (LegacyEntry entry : entries) {
                details.addAll(List.of("legacy", entry.describe()));
            }
            details.addAll(List.of("istio", switch (istioDecision) {
                case IstioRouteTable.Decision.Routed routed -> "ROUTED by " + routed.rule().describe();
                case IstioRouteTable.Decision.Denied denied -> "DENIED by " + denied.rule().describe();
                case IstioRouteTable.Decision.Conflicted conflicted -> "no rule for conflicted PathPrefix " + conflicted.prefix().value();
                case IstioRouteTable.Decision.Unrouted unrouted -> NOT_ROUTED;
            }));
            return details;
        }

        private List<String> exposure(PlannedRule rule) {
            List<String> details = header();
            List<String> fixes = new ArrayList<>();
            List<String> unforbiddable = new ArrayList<>();
            if (legacyDecision instanceof LegacyRouteTable.Decision.Forbidden forbidden) {
                details.addAll(List.of("problem", "legacy forbids this request on " + gateway.refName()
                        + ", and the generated rule routes it"));
                forbidden.entries().stream().filter(e -> e.kind() == LegacyEntry.Kind.IMPLICIT_FORBIDDEN).forEach(entry -> {
                    Optional<String> inexpressible = PathPattern.forbiddenPathProblem(entry.pattern().source());
                    if (inexpressible.isPresent()) {
                        unforbiddable.add("change the gateway path " + entry.pattern().source() + " of "
                                + String.join(", ", entry.route().origins()) + ", which neither @ForbiddenRoute nor an "
                                + "automatic DENY rule can forbid: " + inexpressible.get());
                        return;
                    }
                    Set<Gateway> gateways = EnumSet.allOf(Gateway.class);
                    gateways.removeAll(Gateway.exposedBy(entry.route().route().type()));
                    fixes.add("add " + forbiddenRoute(gateways) + " to " + String.join(", ", entry.route().origins()));
                });
            } else {
                details.addAll(List.of("problem", "legacy doesn't route this request on " + gateway.refName()
                        + ", and the generated rule " + rule.matchType().apiName() + " " + rule.value()
                        + " routes it, because the gateway path was cut at its first variable"));
                fixes.add(cutExposureFix(rule));
            }
            if (fixes.isEmpty() && unforbiddable.isEmpty()) {
                fixes.add("check the @ForbiddenRoute declarations that cover " + probe);
            }
            if (!autoGenerateAuthorizationPolicies && !fixes.isEmpty()) {
                fixes.add("or set <autoGenerateAuthorizationPolicies>true</autoGenerateAuthorizationPolicies> in the "
                        + "plugin configuration to generate the DENY rule automatically");
            }
            fixes.addAll(unforbiddable);
            fixes.forEach(fix -> details.addAll(List.of("fix", fix)));
            return details;
        }

        /**
         * Suggests {@code @ForbiddenRoute} on the class mapped to the cut prefix, or else on a method or class mapped to it,
         * for the gateways on which legacy doesn't route the prefix. Longer routes below the prefix stay allowed,
         * because they become {@code notPaths} of the DENY rule.
         */
        private String cutExposureFix(PlannedRule rule) {
            String prefix = rule.value();
            Set<Gateway> gateways = EnumSet.of(gateway);
            for (Gateway g : rule.gateways()) {
                if (!(legacy.decide(g, prefix) instanceof LegacyRouteTable.Decision.Routed)) {
                    gateways.add(g);
                }
            }
            SortedSet<String> elements = declarations.elementGatewayPaths().getOrDefault(prefix, new TreeSet<>());
            List<String> classes = elements.stream().filter(e -> !e.contains("#")).toList();
            String annotation = forbiddenRoute(gateways);
            if (!classes.isEmpty()) {
                return "add " + annotation + " to class " + String.join(", ", classes);
            }
            if (!elements.isEmpty()) {
                return "add " + annotation + " to one of " + String.join(", ", elements);
            }
            return "add " + annotation + " to a class or method mapped to " + prefix;
        }

        private List<String> conflict(LegacyRouteTable.Decision.Routed legacyRoute, IstioRouteTable.Decision.Routed routed) {
            List<String> details = header();
            SortedSet<String> origins = new TreeSet<>();
            legacyRoute.entries().forEach(e -> origins.addAll(e.route().origins()));
            routed.rule().sources().forEach(r -> origins.addAll(r.origins()));
            details.addAll(List.of(
                    "problem", "legacy forwards this request to " + legacyRoute.upstreamPath()
                            + ", and the generated rule forwards it to " + routed.upstreamPath(),
                    "fix", "align the gateway paths or rewrites of the colliding routes from " + String.join(", ", origins)
                            + ", or migrate the affected waypoint manually to VirtualService outside this plugin"));
            return details;
        }

        private List<String> timeoutChange(LegacyRouteTable.Decision.Routed legacyRoute, IstioRouteTable.Decision.Routed routed) {
            List<String> details = header();
            details.addAll(List.of("problem", "legacy uses timeout " + duration(legacyRoute.timeout())
                    + ", and the generated rule uses " + duration(routed.timeout())));
            return details;
        }

        private List<String> lostRoute() {
            List<String> details = header();
            if (istioDecision instanceof IstioRouteTable.Decision.Denied denied && denied.rule().isAutomatic()) {
                details.addAll(List.of(
                        "problem", "legacy routes this request, and the automatic DENY rule from " + denied.rule().origin()
                                + " denies it",
                        "fix", "change the gateway path of this route or of the routes the DENY rule comes from; "
                                + "the rule was generated automatically, so there is no @ForbiddenRoute declaration to narrow"));
            } else if (istioDecision instanceof IstioRouteTable.Decision.Denied denied) {
                details.addAll(List.of(
                        "problem", "legacy routes this request, and the DENY rule from " + denied.rule().origin() + " denies it",
                        "fix", "narrow the @ForbiddenRoute declaration, or change the gateway path of one of the routes"));
            } else {
                details.addAll(List.of(
                        "problem", "legacy routes this request, and no generated rule routes it",
                        "fix", "change the gateway path of one of the routes involved"));
            }
            return details;
        }

        private Finding undefined(LegacyRouteTable.Decision.Undefined undefined) {
            List<String> details = new ArrayList<>(List.of("request", probe));
            undefined.tied().forEach(entry -> details.addAll(List.of("entry", entry.describe())));
            details.addAll(List.of(
                    "problem", "these legacy entries have gateway paths of the same length that match the request with "
                            + "different results, so legacy picks one of them in an undefined order",
                    "fix", "change the gateway path of one of these routes so that only one of them matches such requests"));
            return Finding.of(Finding.Kind.LEGACY_PRECEDENCE_UNDEFINED, gateway.refName(), details.toArray(String[]::new));
        }
    }

    static String forbiddenRoute(Set<Gateway> gateways) {
        List<String> types = Arrays.stream(Gateway.values()).filter(gateways::contains)
                .map(g -> "RouteType." + g.routeType().name()).toList();
        return types.size() == 1 ? "@ForbiddenRoute(" + types.get(0) + ")"
                : "@ForbiddenRoute({" + String.join(", ", types) + "})";
    }

    private static String duration(long timeout) {
        return timeout > 0 ? HttpRouteRenderer.formatDuration(timeout) : "none";
    }
}
