package com.netcracker.routes.gateway.plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Plans the Istio resources for the declared routes (design D6–D8, D12, D15):
 * every gateway path is cut at its first variable into a {@code PathPrefix}, routes with the same match share one rule,
 * bare-root routes are split into {@code Exact} rules where needed, and forbidden paths become DENY rules.
 */
public final class IstioRoutePlanner {

    private final boolean autoGenerateAuthorizationPolicies;

    public IstioRoutePlanner(boolean autoGenerateAuthorizationPolicies) {
        this.autoGenerateAuthorizationPolicies = autoGenerateAuthorizationPolicies;
    }

    public IstioPlan plan(RouteDeclarations declarations, LegacyRouteTable legacy) {
        String sample = Probes.sample(declarations);
        List<DeclaredRoute> border = declarations.routes().stream()
                .filter(r -> r.route().type() != HttpRoute.Type.FACADE).toList();
        List<DeclaredRoute> facade = declarations.routes().stream()
                .filter(r -> r.route().type() == HttpRoute.Type.FACADE).toList();

        Plan borderPlan = new BorderPlan(sample);
        borderPlan.addAll(border);
        Plan servicePlan = new ServicePlan(sample);
        servicePlan.addAll(facade);
        if (servicePlan.rules.stream().noneMatch(PlannedRule::hasRewrite)) {
            servicePlan.rules.clear();
            List<DeclaredRoute> timed = facade.stream().filter(r -> r.route().timeout() > 0).toList();
            if (!timed.isEmpty()) {
                servicePlan.findings.add(timeoutNotApplied(timed));
            }
        }

        List<PlannedRule> rules = new ArrayList<>(borderPlan.rules);
        rules.addAll(servicePlan.rules);
        List<IstioPlan.ConflictedPrefix> conflicted = new ArrayList<>(borderPlan.conflicted);
        conflicted.addAll(servicePlan.conflicted);
        List<Finding> findings = new ArrayList<>(borderPlan.findings);
        findings.addAll(servicePlan.findings);
        return new IstioPlan(rules, conflicted, denyRules(legacy, rules, conflicted, sample), findings, sample);
    }

    // DENY rules (D8, D12)

    private List<DenyRule> denyRules(LegacyRouteTable legacy, List<PlannedRule> routingRules,
                                     List<IstioPlan.ConflictedPrefix> conflicted, String sample) {
        Map<List<Object>, DenyRule> rules = new LinkedHashMap<>();
        for (Gateway gateway : Gateway.values()) {
            for (LegacyEntry entry : legacy.entries(gateway)) {
                if (entry.kind() == LegacyEntry.Kind.EXPLICIT_FORBIDDEN) {
                    add(rules, denyRule(gateway, entry.pattern(), legacy, entry.declaration().origin()));
                }
            }
        }
        if (autoGenerateAuthorizationPolicies) {
            IstioRouteTable routing = IstioRouteTable.routingOnly(routingRules, conflicted);
            for (Gateway gateway : Gateway.values()) {
                for (LegacyEntry entry : legacy.entries(gateway)) {
                    // a path @ForbiddenRoute can't express gets no rule either; the validator reports its exposure
                    if (entry.kind() != LegacyEntry.Kind.IMPLICIT_FORBIDDEN
                            || PathPattern.forbiddenPathProblem(entry.pattern().source()).isPresent()) {
                        continue;
                    }
                    String request = entry.pattern().instantiate(sample);
                    if (isRouted(routing.decide(gateway, request)) || isRouted(routing.decide(gateway, Probes.child(request, sample)))) {
                        add(rules, denyRule(gateway, entry.pattern(), legacy,
                                DenyRule.AUTO_ORIGIN_PREFIX + "implicit forbidden of " + entry.route().describe()));
                    }
                }
            }
            for (PlannedRule rule : routingRules) {
                boolean cut = rule.matchType() == PlannedRule.MatchType.PATH_PREFIX
                        && rule.sources().stream().anyMatch(r -> r.route().gatewayPath().contains("{"));
                if (!cut) {
                    continue;
                }
                for (Gateway gateway : rule.gateways()) {
                    // a shorter route that routes paths below P routes P too; longer routes below P become notPaths
                    if (!(legacy.decide(gateway, rule.value()) instanceof LegacyRouteTable.Decision.Routed)) {
                        String origins = rule.sources().stream().flatMap(r -> r.origins().stream()).distinct().sorted()
                                .collect(Collectors.joining(", "));
                        add(rules, denyRule(gateway, PathPattern.of(rule.value()), legacy,
                                DenyRule.AUTO_ORIGIN_PREFIX + "cut exposure of " + rule.value() + " from " + origins));
                    }
                }
            }
        }
        return rules.values().stream()
                .sorted(Comparator.comparing(DenyRule::gateway)
                        .thenComparing(r -> String.join(" ", r.paths()))
                        .thenComparing(r -> String.join(" ", r.notPaths())))
                .toList();
    }

    private static boolean isRouted(IstioRouteTable.Decision decision) {
        return decision instanceof IstioRouteTable.Decision.Routed;
    }

    private static void add(Map<List<Object>, DenyRule> rules, DenyRule rule) {
        rules.putIfAbsent(List.of(rule.gateway(), rule.paths(), rule.notPaths()), rule);
    }

    /**
     * {@code paths} are the forbidden path and its subtree; {@code notPaths} are the longer allowed routes that match
     * some of the same requests, which won over the forbidden path in legacy.
     */
    static DenyRule denyRule(Gateway gateway, PathPattern forbidden, LegacyRouteTable legacy, String origin) {
        Set<String> notPaths = new TreeSet<>();
        for (LegacyEntry entry : legacy.entries(gateway)) {
            if (entry.isAllowed() && entry.pattern().length() > forbidden.length() && forbidden.overlaps(entry.pattern())) {
                notPaths.addAll(DenyRule.pathAndSubtree(entry.pattern().istioTemplate()));
            }
        }
        return new DenyRule(gateway, DenyRule.pathAndSubtree(forbidden.istioTemplate()), List.copyOf(notPaths), origin);
    }

    // HTTPRoute rules (D6, D7, D15)

    /**
     * @return the prefix rewrite of a route cut to {@code match}, or empty when the route needs no rewrite
     */
    static Optional<String> rewriteOf(HttpRoute route, String match) {
        if (route.path().equals(route.gatewayPath())) {
            return Optional.empty();
        }
        String rewrite = PathPattern.cut(route.path());
        return rewrite.equals(match) ? Optional.empty() : Optional.of(rewrite);
    }

    /**
     * Builds the rules of one set of HTTPRoute resources: the border ones or the service-bound one.
     */
    private abstract static class Plan {
        final String sample;
        final List<PlannedRule> rules = new ArrayList<>();
        final List<IstioPlan.ConflictedPrefix> conflicted = new ArrayList<>();
        final List<Finding> findings = new ArrayList<>();

        Plan(String sample) {
            this.sample = sample;
        }

        /**
         * @return the resource the rule for these routes goes into
         */
        abstract HttpRoute.Type resourceType(List<DeclaredRoute> routes);

        /**
         * @return the finding target for a rule
         */
        abstract String target(PlannedRule rule);

        /**
         * @return the {@code CONFLICT} finding for routes that are cut to one {@code PathPrefix} and need different rewrites
         */
        abstract Finding conflict(String match, List<DeclaredRoute> routes);

        /**
         * Groups the routes by cut gateway path, across route types: Istio can't tell apart two rules with one
         * {@code PathPrefix} value, even in different resources. A shared rule goes into the widest type's resource,
         * and DENY rules and the validator take care of the gateways the narrower routes aren't exposed on.
         */
        void addAll(List<DeclaredRoute> routes) {
            Map<String, List<DeclaredRoute>> groups = new TreeMap<>();
            for (DeclaredRoute route : routes) {
                groups.computeIfAbsent(PathPattern.cut(route.route().gatewayPath()), m -> new ArrayList<>()).add(route);
            }
            groups.forEach(this::addGroup);
        }

        private void addGroup(String match, List<DeclaredRoute> routes) {
            Set<Optional<String>> rewrites = rewrites(match, routes);
            if (rewrites.size() == 1 && routes.stream().map(r -> r.route().timeout()).distinct().count() == 1) {
                rules.add(prefixRule(match, routes));
                return;
            }
            Optional<List<DeclaredRoute>> split = exactSplit(match, routes);
            if (split.isPresent()) {
                DeclaredRoute root = split.get().get(0);
                List<DeclaredRoute> rest = split.get().subList(1, split.get().size());
                rules.addAll(exactRules(match, root));
                addMerged(match, rest);
                return;
            }
            if (rewrites.size() == 1) {
                addMerged(match, routes);
                return;
            }
            conflicted.add(new IstioPlan.ConflictedPrefix(match, resourceType(routes)));
            findings.add(conflict(match, routes));
        }

        Set<Optional<String>> rewrites(String match, List<DeclaredRoute> routes) {
            return routes.stream().map(r -> rewriteOf(r.route(), match)).collect(Collectors.toSet());
        }

        /**
         * D7 step 2: exactly one route S has gateway path {@code match} (with or without a trailing slash),
         * another has {@code match/{var}}, and all routes but S share one rewrite.
         *
         * @return S followed by the other routes
         */
        private Optional<List<DeclaredRoute>> exactSplit(String match, List<DeclaredRoute> routes) {
            // in a group, the gateway paths without variables are the ones cut to match unchanged
            List<DeclaredRoute> roots = routes.stream().filter(r -> !r.route().gatewayPath().contains("{")).toList();
            if (roots.size() != 1) {
                return Optional.empty();
            }
            DeclaredRoute root = roots.get(0);
            List<DeclaredRoute> rest = routes.stream().filter(r -> r != root).toList();
            String childPrefix = match.endsWith("/") ? match : match + "/";
            boolean variableChild = rest.stream().map(r -> r.route().gatewayPath())
                    .anyMatch(p -> p.startsWith(childPrefix) && PathPattern.isVariable(p.substring(childPrefix.length())));
            if (!variableChild || rewrites(match, rest).size() != 1) {
                return Optional.empty();
            }
            List<DeclaredRoute> result = new ArrayList<>();
            result.add(root);
            result.addAll(rest);
            return Optional.of(result);
        }

        private List<PlannedRule> exactRules(String match, DeclaredRoute root) {
            HttpRoute route = root.route();
            HttpRoute.Type type = resourceType(List.of(root));
            boolean rewrite = !route.path().equals(route.gatewayPath());
            List<PlannedRule> exact = new ArrayList<>();
            List<String> values = match.equals("/") ? List.of("/") : List.of(match, match + "/");
            for (String value : values) {
                String remainder = value.equals(match) ? "" : "/";
                exact.add(new PlannedRule(type, PlannedRule.MatchType.EXACT, value,
                        rewrite ? PlannedRule.RewriteType.REPLACE_FULL_PATH : null,
                        rewrite ? PathPattern.appendRemainder(route.path(), remainder) : null,
                        route.timeout(), List.of(root)));
            }
            return exact;
        }

        /**
         * Merges routes with one rewrite into one rule with the largest timeout, and warns if their timeouts differ.
         */
        private void addMerged(String match, List<DeclaredRoute> routes) {
            PlannedRule rule = prefixRule(match, routes);
            rules.add(rule);
            if (routes.stream().map(r -> r.route().timeout()).distinct().count() > 1) {
                findings.add(timeoutMerge(rule));
            }
        }

        private PlannedRule prefixRule(String match, List<DeclaredRoute> routes) {
            Optional<String> rewrite = rewriteOf(routes.get(0).route(), match);
            long timeout = routes.stream().mapToLong(r -> r.route().timeout()).max().orElse(0);
            return new PlannedRule(resourceType(routes), PlannedRule.MatchType.PATH_PREFIX, match,
                    rewrite.map(r -> PlannedRule.RewriteType.REPLACE_PREFIX_MATCH).orElse(null),
                    rewrite.orElse(null), timeout, routes);
        }

        /**
         * @return the details of a {@code CONFLICT} finding up to its problem
         */
        List<String> conflictDetails(String match, List<DeclaredRoute> routes) {
            DeclaredRoute longest = routes.stream()
                    .max(Comparator.comparingInt((DeclaredRoute r) -> r.route().gatewayPath().length())
                            .thenComparing(r -> r.route().gatewayPath(), Comparator.reverseOrder()))
                    .orElseThrow();
            List<String> details = new ArrayList<>(List.of(
                    "match", "PathPrefix " + match,
                    "request", PathPattern.of(longest.route().gatewayPath()).instantiate(sample)));
            addRoutes(details, match, routes);
            return details;
        }

        static Finding conflictFinding(String target, List<String> details, List<DeclaredRoute> routes) {
            return Finding.of(Finding.Kind.CONFLICT, target, details.toArray(String[]::new))
                    .about(routes.stream().map(r -> r.route().gatewayPath()).toList());
        }

        private Finding timeoutMerge(PlannedRule rule) {
            List<String> details = new ArrayList<>(List.of("match", rule.matchType().apiName() + " " + rule.value()));
            addRoutes(details, rule.value(), rule.sources());
            details.addAll(List.of("timeout", "the merged rule uses the largest timeout, "
                    + HttpRouteRenderer.formatDuration(rule.timeout())));
            return Finding.of(Finding.Kind.TIMEOUT_MERGE, target(rule), details.toArray(String[]::new));
        }

        private static void addRoutes(List<String> details, String match, List<DeclaredRoute> routes) {
            routes.stream()
                    .sorted(Comparator.comparing((DeclaredRoute r) -> r.route().gatewayPath()).thenComparing(DeclaredRoute::originsText))
                    .forEach(route -> {
                        details.add("route");
                        String rewrite = rewriteOf(route.route(), match).map(r -> "ReplacePrefixMatch " + r).orElse("no rewrite");
                        String timeout = route.route().timeout() > 0
                                ? ", timeout " + HttpRouteRenderer.formatDuration(route.route().timeout()) : "";
                        details.add(route.describe() + " [" + rewrite + timeout + "]");
                    });
        }
    }

    /**
     * The public, private and internal HTTPRoutes: a rule goes into the resource of the widest route type it serves.
     */
    private static final class BorderPlan extends Plan {

        BorderPlan(String sample) {
            super(sample);
        }

        @Override
        HttpRoute.Type resourceType(List<DeclaredRoute> routes) {
            return Gateway.widest(routes.stream().map(r -> r.route().type()).toList());
        }

        @Override
        String target(PlannedRule rule) {
            return targetName(rule.gateways());
        }

        @Override
        Finding conflict(String match, List<DeclaredRoute> routes) {
            List<String> details = conflictDetails(match, routes);
            details.addAll(List.of(
                    "problem", "these routes are cut to PathPrefix " + match + " and need different rewrites, "
                            + "which one Istio rule can't express",
                    "fix", "align the gateway paths or service paths of these routes so that they share one rewrite, "
                            + "or migrate the affected waypoint manually to VirtualService outside this plugin"));
            return conflictFinding(conflictGateways(match, routes), details, routes);
        }

        /**
         * @return the gateways on which routes with different rewrites are allowed together
         */
        private String conflictGateways(String match, List<DeclaredRoute> routes) {
            Set<Gateway> gateways = EnumSet.noneOf(Gateway.class);
            for (Gateway gateway : Gateway.values()) {
                List<DeclaredRoute> allowed = routes.stream()
                        .filter(r -> Gateway.exposedBy(r.route().type()).contains(gateway)).toList();
                if (rewrites(match, allowed).size() > 1) {
                    gateways.add(gateway);
                }
            }
            return targetName(gateways);
        }
    }

    /**
     * The service-bound HTTPRoute that all facade and composite routes share.
     */
    private static final class ServicePlan extends Plan {

        ServicePlan(String sample) {
            super(sample);
        }

        @Override
        HttpRoute.Type resourceType(List<DeclaredRoute> routes) {
            return HttpRoute.Type.FACADE;
        }

        @Override
        String target(PlannedRule rule) {
            return Finding.SERVICE_TARGET;
        }

        @Override
        Finding conflict(String match, List<DeclaredRoute> routes) {
            List<String> details = conflictDetails(match, routes);
            details.addAll(List.of(
                    "problem", "facade and composite routes of all legacy facade and composite gateways share one "
                            + "service-bound HTTPRoute, so routes that came from different legacy gateways collide "
                            + "here; these routes are cut to PathPrefix " + match + " and need different rewrites",
                    "fix", "align the gateway paths or service paths of these routes, or migrate the Service's "
                            + "waypoint manually to VirtualService outside this plugin"));
            return conflictFinding(Finding.SERVICE_TARGET, details, routes);
        }
    }

    static String targetName(Set<Gateway> gateways) {
        return gateways.stream().sorted().map(Gateway::refName).collect(Collectors.joining(", "));
    }

    private static Finding timeoutNotApplied(List<DeclaredRoute> routes) {
        List<String> details = new ArrayList<>();
        routes.forEach(route -> {
            details.add("route");
            details.add(route.describe() + " [timeout " + HttpRouteRenderer.formatDuration(route.route().timeout()) + "]");
        });
        details.addAll(List.of("problem", "no facade or composite route has a rewrite, so no service-bound HTTPRoute is "
                + "generated, and the timeouts of these routes are not applied"));
        return Finding.of(Finding.Kind.TIMEOUT_NOT_APPLIED, Finding.SERVICE_TARGET, details.toArray(String[]::new));
    }
}
