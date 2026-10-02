package com.netcracker.routes.gateway.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Renders one DENY {@code AuthorizationPolicy} per external gateway, public then private. Exposure on the internal
 * gateway needs no rule, like that of facade routes.
 * <p>
 * Legacy gateways forbid a route on the gateways wider than its type, and don't route a path no route matches.
 * Istio routes by the {@code PathPrefix} cut from each gateway path, so it routes some of these paths:
 * <ul>
 *     <li>a narrower route's path below the prefix of a route exposed on the gateway;</li>
 *     <li>the prefix itself, cut from a route with variables, if no route exposed on the gateway matches it.</li>
 * </ul>
 * These paths need DENY rules. They are generated if {@code autoGenerateAuthorizationPolicies} is set, else they must
 * be declared with {@code @ForbiddenRoute}, as all other forbidden paths.
 */
public class AuthorizationPolicyRenderer {

    private static final List<String> PORTS = List.of("8080");
    private static final String ACTION_DENY = "DENY";
    private static final List<HttpRoute.Type> GATEWAYS =
            List.of(HttpRoute.Type.PUBLIC, HttpRoute.Type.PRIVATE);

    private final Map<String, String> labels;
    private final boolean autoGenerateAuthorizationPolicies;

    public AuthorizationPolicyRenderer(Map<String, String> labels, boolean autoGenerateAuthorizationPolicies) {
        this.labels = labels == null ? Collections.emptyMap() : labels;
        this.autoGenerateAuthorizationPolicies = autoGenerateAuthorizationPolicies;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AuthorizationPolicyResource(String apiVersion, String kind, Metadata metadata, Spec spec) {

        public AuthorizationPolicyResource(Metadata metadata, Spec spec) {
            this("security.istio.io/v1", "AuthorizationPolicy", metadata, spec);
        }

        public record Metadata(String name, Map<String, String> labels) {
        }

        public record Spec(List<TargetRef> targetRefs, String action, List<Rule> rules) {

            public record TargetRef(String group, String kind, String name) {
            }

            public record Rule(List<To> to) {

                public record To(Operation operation) {
                }

                @JsonInclude(JsonInclude.Include.NON_EMPTY)
                public record Operation(List<String> ports, List<String> paths, List<String> notPaths) {
                }
            }
        }
    }

    /**
     * A path forbidden on a gateway that Istio would route without a DENY rule.
     */
    private record Missing(String reason, Set<HttpRoute.Type> gateways) {
    }

    public String generateAuthorizationPoliciesYaml(Set<HttpRoute> routes, Set<ForbiddenPath> forbidden, Problems problems) {
        List<HttpRoute> border = routes.stream().filter(r -> r.type() != HttpRoute.Type.FACADE).toList();
        Map<String, Missing> missing = new TreeMap<>();
        Set<String> inexpressible = new TreeSet<>();
        StringBuilder yaml = new StringBuilder();
        for (HttpRoute.Type gateway : GATEWAYS) {
            Collection<AuthorizationPolicyResource.Spec.Rule> rules =
                    denyRules(gateway, border, forbidden, missing, inexpressible, problems);
            if (!rules.isEmpty()) {
                yaml.append(HttpRouteRenderer.writeYaml(toResource(gateway, List.copyOf(rules))));
            }
        }
        missing.forEach((path, m) -> problems.error(path + " " + m.reason() + " on " + gatewayNames(m.gateways())
                + ": add @ForbiddenRoute({" + m.gateways().stream().sorted(Comparator.reverseOrder()).map(Enum::name)
                .collect(Collectors.joining(", ")) + "}) to the element mapped to " + path
                + ", or set autoGenerateAuthorizationPolicies to generate the DENY rules"));
        inexpressible.forEach(path -> problems.error(path + " is a forbidden path or overlaps one, and an "
                + "AuthorizationPolicy can't express it: variables must take up whole path segments"));
        return yaml.toString();
    }

    private Collection<AuthorizationPolicyResource.Spec.Rule> denyRules(HttpRoute.Type gateway, List<HttpRoute> border,
                                                                        Set<ForbiddenPath> forbidden,
                                                                        Map<String, Missing> missing,
                                                                        Set<String> inexpressible, Problems problems) {
        Set<String> allowed = border.stream().filter(r -> r.type().compareTo(gateway) >= 0)
                .map(HttpRoute::gatewayPath).collect(Collectors.toCollection(TreeSet::new));
        Set<String> allowedTemplates = allowed.stream().map(RoutePaths::template).collect(Collectors.toSet());
        Set<String> implicit = border.stream().filter(r -> r.type().compareTo(gateway) < 0)
                .map(HttpRoute::gatewayPath).filter(p -> !allowedTemplates.contains(RoutePaths.template(p)))
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> explicit = forbidden.stream().filter(f -> f.gateways().contains(gateway))
                .map(ForbiddenPath::gatewayPath).collect(Collectors.toCollection(TreeSet::new));
        explicit.stream().filter(p -> allowedTemplates.contains(RoutePaths.template(p))).forEach(p -> problems.error(
                "@ForbiddenRoute forbids " + p + " on " + gateway.gatewayName() + ", where a route with this gateway path is exposed"));

        Map<String, String> needed = new TreeMap<>();
        Set<String> prefixes = allowed.stream().map(RoutePaths::cut).collect(Collectors.toCollection(TreeSet::new));
        for (String path : implicit) {
            if (legacyRoute(RoutePaths.sample(path, path), allowed, implicit).filter(allowed::contains).isPresent()) {
                continue;
            }
            prefixes.stream().filter(prefix -> RoutePaths.covers(prefix, path)).findFirst().ifPresent(prefix -> needed.put(path,
                    "is forbidden by legacy, as its route type is narrower, but Istio routes it by PathPrefix " + prefix));
        }
        for (String path : allowed) {
            String prefix = RoutePaths.cut(path);
            if (!RoutePaths.hasVariable(path) || needed.containsKey(prefix)) {
                continue;
            }
            if (legacyRoute(prefix, allowed, implicit).filter(allowed::contains).isEmpty()) {
                needed.put(prefix, "is not routed by legacy, but Istio routes it by PathPrefix " + prefix + " cut from " + path);
            }
        }

        Set<String> explicitTemplates = explicit.stream().map(RoutePaths::template).collect(Collectors.toSet());
        Set<String> rulePaths = new TreeSet<>(explicit);
        needed.forEach((path, reason) -> {
            if (autoGenerateAuthorizationPolicies) {
                rulePaths.add(path);
            } else if (!explicitTemplates.contains(RoutePaths.template(path))) {
                missing.computeIfAbsent(path, p -> new Missing(reason, EnumSet.noneOf(HttpRoute.Type.class)))
                        .gateways().add(gateway);
            }
        });

        Map<String, AuthorizationPolicyResource.Spec.Rule> rules = new TreeMap<>();
        for (String path : rulePaths) {
            List<String> overlapping = allowed.stream()
                    .filter(route -> route.length() > path.length() && RoutePaths.overlaps(path, route)).toList();
            // Probes the rule path and its intersection with every overlapping allowed route: the route legacy routes
            // a probe by, if the rule denies the probe, is excluded too, so that the rule denies nothing legacy routed
            Set<String> excluded = new TreeSet<>(overlapping);
            Stream.concat(Stream.of(path), allowed.stream().filter(route -> RoutePaths.overlaps(path, route)))
                    .map(route -> RoutePaths.sample(path, route))
                    .filter(request -> overlapping.stream().noneMatch(route -> RoutePaths.covers(route, request)))
                    .forEach(request -> legacyRoute(request, allowed, implicit).filter(allowed::contains)
                            .ifPresent(excluded::add));
            if (explicit.contains(path)) {
                // An explicit rule denies its path even where legacy routed it by a wider route
                excluded.removeIf(route -> RoutePaths.covers(route, RoutePaths.sample(path, path)));
            }
            List<String> invalid = Stream.concat(Stream.of(path), excluded.stream())
                    .filter(p -> !RoutePaths.expressible(p)).toList();
            if (!invalid.isEmpty()) {
                inexpressible.addAll(invalid);
                continue;
            }
            List<String> notPaths = excluded.stream().map(RoutePaths::template).distinct().sorted()
                    .flatMap(t -> subtree(t).stream()).toList();
            rules.putIfAbsent(RoutePaths.template(path), new AuthorizationPolicyResource.Spec.Rule(List.of(
                    new AuthorizationPolicyResource.Spec.Rule.To(new AuthorizationPolicyResource.Spec.Rule.Operation(
                            PORTS, subtree(RoutePaths.template(path)), notPaths)))));
        }
        return rules.values();
    }

    /**
     * @return the route legacy matches a literal request path by: the longest covering one, the forbidden one on a tie
     */
    private static Optional<String> legacyRoute(String request, Set<String> allowed, Set<String> implicit) {
        return Stream.concat(allowed.stream(), implicit.stream()).filter(route -> RoutePaths.covers(route, request))
                .max(Comparator.comparingInt(String::length).thenComparing(route -> !allowed.contains(route)));
    }

    private static List<String> subtree(String template) {
        return List.of(template, template.equals("/") ? "/{**}" : template + "/{**}");
    }

    private static String gatewayNames(Set<HttpRoute.Type> gateways) {
        return gateways.stream().sorted(Comparator.reverseOrder()).map(HttpRoute.Type::gatewayName)
                .collect(Collectors.joining(", "));
    }

    private AuthorizationPolicyResource toResource(HttpRoute.Type gateway, List<AuthorizationPolicyResource.Spec.Rule> rules) {
        AuthorizationPolicyResource.Metadata metadata = new AuthorizationPolicyResource.Metadata(
                "{{ .Values.SERVICE_NAME }}-java-annotations-deny-" + gateway.name().toLowerCase(),
                HttpRouteRenderer.buildRouteLabels(labels));
        HttpRouteRenderer.HTTPRouteResource.Spec.ParentRef ref = HttpRouteRenderer.gatewayParentRef(gateway.gatewayName());
        AuthorizationPolicyResource.Spec.TargetRef targetRef =
                new AuthorizationPolicyResource.Spec.TargetRef(ref.group(), ref.kind(), ref.name());
        return new AuthorizationPolicyResource(metadata,
                new AuthorizationPolicyResource.Spec(List.of(targetRef), ACTION_DENY, rules));
    }
}
