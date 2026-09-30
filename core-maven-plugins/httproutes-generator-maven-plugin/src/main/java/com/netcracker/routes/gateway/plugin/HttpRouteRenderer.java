package com.netcracker.routes.gateway.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpRouteRenderer {

    private static final ObjectMapper YAML_MAPPER = yamlMapper();
    private static final String MATCH_TYPE_PATH_PREFIX = "PathPrefix";
    private static final String MATCH_TYPE_EXACT = "Exact";
    private static final String FILTER_TYPE_URL_REWRITE = "URLRewrite";
    private static final String FILTER_PATH_TYPE_REPLACE_PREFIX_MATCH = "ReplacePrefixMatch";
    private static final String FILTER_PATH_TYPE_REPLACE_FULL_PATH = "ReplaceFullPath";
    private static final List<HttpRoute.Type> RESOURCE_ORDER =
            List.of(HttpRoute.Type.PUBLIC, HttpRoute.Type.PRIVATE, HttpRoute.Type.INTERNAL, HttpRoute.Type.FACADE);

    private static final long SECOND = 1_000;
    private static final long MINUTE = 60_000;
    private static final long HOUR = 3_600_000;
    public static final String PARENT_REF_KIND_SERVICE = "Service";
    public static final String PARENT_REF_KIND_GATEWAY = "Gateway";
    public static final String PARENT_REF_GROUP_GATEWAY = "gateway.networking.k8s.io";
    public static final String PARENT_REF_GROUP_SERVICE = "";
    private static final Map<String, String> DEFAULT_ROUTE_LABELS = Map.of(
            "app.kubernetes.io/name", "{{ .Values.SERVICE_NAME }}",
            "app.kubernetes.io/part-of", "{{ .Values.APPLICATION_NAME }}",
            "app.kubernetes.io/managed-by", "{{ .Values.MANAGED_BY }}",
            "deployment.netcracker.com/sessionId", "{{ .Values.DEPLOYMENT_SESSION_ID }}",
            "deployer.cleanup/allow", "true",
            "app.kubernetes.io/processed-by-operator", "istiod"
    );

    private final String backendRefVal;
    private final Map<String, String> routeLabels;

    public HttpRouteRenderer(String backendRefVal) {
        this(backendRefVal, Collections.emptyMap());
    }

    public HttpRouteRenderer(String backendRefVal, Map<String, String> routeLabels) {
        this.backendRefVal = backendRefVal;
        this.routeLabels = routeLabels == null ? Collections.emptyMap() : routeLabels;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HTTPRouteResource(String apiVersion, String kind, Metadata metadata, Spec spec) {

        public HTTPRouteResource(Metadata metadata, Spec spec) {
            this("gateway.networking.k8s.io/v1", "HTTPRoute", metadata, spec);
        }

        public record Metadata(String name, Map<String, String> labels) {
        }

        public record Spec(Set<ParentRef> parentRefs, List<Rule> rules) {

            public record ParentRef(String group, String kind, String name) {
            }

            public record Rule(
                    List<Match> matches,
                    List<Filter> filters,
                    List<BackendRef> backendRefs,
                    Timeouts timeouts
            ) {
                public record Timeouts(String request) {
                }
            }

            public record Match(Path path) {
                public record Path(String type, String value) {
                }
            }

            public record Filter(String type, UrlRewrite urlRewrite) {
                public record UrlRewrite(Path path) {
                    public record Path(String type, String replacePrefixMatch, String replaceFullPath) {
                    }
                }
            }

            public record BackendRef(String group, String kind, String name, Integer port, Integer weight) {
            }
        }
    }

    /**
     * One rule of the generated HTTPRoutes.
     *
     * @param resource  resource the rule goes into; {@code FACADE} is the service-bound HTTPRoute
     * @param rewrite   {@code ReplacePrefixMatch} value for {@code PathPrefix}, {@code ReplaceFullPath} value for
     *                  {@code Exact}, or {@code null} for no rewrite
     */
    private record PlannedRule(HttpRoute.Type resource, String matchType, String value, String rewrite, long timeout) {
    }

    /**
     * Cuts every gateway path at its first variable into a {@code PathPrefix} and merges the routes cut to one
     * prefix into one rule. Conflicts are reported to {@code problems}.
     */
    public String generateHttpRoutesYaml(int servicePort, Set<HttpRoute> httpRoutes, Problems problems) {
        List<PlannedRule> rules = planRules(httpRoutes.stream().filter(r -> r.type() != HttpRoute.Type.FACADE), problems);
        List<PlannedRule> facadeRules = planRules(httpRoutes.stream().filter(r -> r.type() == HttpRoute.Type.FACADE), problems);
        if (facadeRules.stream().anyMatch(r -> r.rewrite() != null)) {
            rules.addAll(facadeRules);
        } else if (facadeRules.stream().anyMatch(r -> r.timeout() > 0)) {
            problems.warn("No facade or composite route has a rewrite, so no service-bound HTTPRoute is generated, "
                    + "and the timeouts of the facade and composite routes are not applied");
        }

        List<HTTPRouteResource> resources = new ArrayList<>();
        for (HttpRoute.Type type : RESOURCE_ORDER) {
            List<PlannedRule> typeRules = rules.stream().filter(r -> r.resource() == type).toList();
            if (!typeRules.isEmpty()) {
                resources.add(toResource(type, typeRules, servicePort));
            }
        }
        return resources.stream()
                .map(HttpRouteRenderer::writeYaml)
                .collect(Collectors.joining());
    }

    /**
     * Groups the routes by cut gateway path across route types, because Istio can't tell apart two rules with one
     * {@code PathPrefix} value. A group's rule goes into the widest type's resource; DENY rules take care of the
     * gateways the narrower routes aren't exposed on.
     */
    private static List<PlannedRule> planRules(Stream<HttpRoute> routes, Problems problems) {
        Map<String, List<HttpRoute>> groups = routes
                .sorted(Comparator.comparing(HttpRoute::gatewayPath).thenComparing(HttpRoute::path)
                        .thenComparing(HttpRoute::type).thenComparingLong(HttpRoute::timeout))
                .collect(Collectors.groupingBy(r -> RoutePaths.cut(r.gatewayPath()), TreeMap::new, Collectors.toList()));
        List<PlannedRule> rules = new ArrayList<>();
        groups.forEach((match, group) -> {
            if (rewrites(match, group).size() == 1) {
                rules.add(prefixRule(match, group, problems));
                return;
            }
            Optional<HttpRoute> root = exactSplitRoot(match, group);
            if (root.isPresent()) {
                rules.addAll(exactRules(match, root.get()));
                rules.add(prefixRule(match, group.stream().filter(r -> r != root.get()).toList(), problems));
                return;
            }
            problems.error("Routes " + describe(match, group) + " are cut to PathPrefix " + match
                    + " and need different rewrites, which one Istio rule can't express. Align their gateway paths or "
                    + "service paths so that they share one rewrite");
        });
        return rules;
    }

    /**
     * @return the {@code ReplacePrefixMatch} value of a route cut to {@code match}, or empty for no rewrite
     */
    private static Optional<String> rewriteOf(String match, HttpRoute route) {
        String rewrite = RoutePaths.cut(route.path());
        return rewrite.equals(match) ? Optional.empty() : Optional.of(rewrite);
    }

    private static Set<Optional<String>> rewrites(String match, List<HttpRoute> routes) {
        return routes.stream().map(r -> rewriteOf(match, r)).collect(Collectors.toSet());
    }

    /**
     * The routes of a group can be split when exactly one route S has gateway path {@code match} (or {@code match/}),
     * another has {@code match/{var}}, and all but S share one rewrite: S then gets {@code Exact} rules.
     *
     * @return S, or empty if the routes can't be split
     */
    private static Optional<HttpRoute> exactSplitRoot(String match, List<HttpRoute> routes) {
        List<HttpRoute> roots = routes.stream().filter(r -> !RoutePaths.hasVariable(r.gatewayPath())).toList();
        if (roots.size() != 1) {
            return Optional.empty();
        }
        List<HttpRoute> rest = routes.stream().filter(r -> r != roots.get(0)).toList();
        int depth = RoutePaths.segments(match).size();
        boolean variableChild = rest.stream().map(r -> RoutePaths.segments(r.gatewayPath()))
                .anyMatch(segments -> segments.size() == depth + 1 && RoutePaths.template(segments.get(depth)).equals("/{*}"));
        return variableChild && rewrites(match, rest).size() == 1 ? Optional.of(roots.get(0)) : Optional.empty();
    }

    private static List<PlannedRule> exactRules(String match, HttpRoute root) {
        boolean rewrite = rewriteOf(match, root).isPresent();
        String path = RoutePaths.cut(root.path());
        List<PlannedRule> rules = new ArrayList<>();
        rules.add(new PlannedRule(root.type(), MATCH_TYPE_EXACT, match, rewrite ? path : null, root.timeout()));
        if (!match.equals("/")) {
            rules.add(new PlannedRule(root.type(), MATCH_TYPE_EXACT, match + "/",
                    rewrite ? (path.equals("/") ? path : path + "/") : null, root.timeout()));
        }
        return rules;
    }

    /**
     * Merges routes with one rewrite into one rule with the largest timeout, and warns if their timeouts differ.
     */
    private static PlannedRule prefixRule(String match, List<HttpRoute> routes, Problems problems) {
        HttpRoute.Type widest = routes.stream().map(HttpRoute::type).max(Comparator.naturalOrder()).orElseThrow();
        long timeout = routes.stream().mapToLong(HttpRoute::timeout).max().orElse(0);
        if (routes.stream().mapToLong(HttpRoute::timeout).distinct().count() > 1) {
            problems.warn("Routes " + describe(match, routes) + " are merged into one rule PathPrefix " + match
                    + " with the largest timeout " + formatDuration(timeout));
        }
        return new PlannedRule(widest, MATCH_TYPE_PATH_PREFIX, match, rewriteOf(match, routes.get(0)).orElse(null), timeout);
    }

    private static String describe(String match, List<HttpRoute> routes) {
        return routes.stream()
                .map(r -> r.gatewayPath() + " (" + r.type() + ", "
                        + rewriteOf(match, r).map(rewrite -> "ReplacePrefixMatch " + rewrite).orElse("no rewrite")
                        + (r.timeout() > 0 ? ", timeout " + formatDuration(r.timeout()) : "") + ")")
                .collect(Collectors.joining(", "));
    }

    public static String formatDuration(long ms) {
        if (ms % HOUR == 0) {
            return (ms / HOUR) + "h";
        }
        if (ms % MINUTE == 0) {
            return (ms / MINUTE) + "m";
        }
        if (ms % SECOND == 0) {
            return (ms / SECOND) + "s";
        }
        return ms + "ms";
    }

    private HTTPRouteResource toResource(HttpRoute.Type type, List<PlannedRule> rules, int servicePort) {
        HTTPRouteResource.Metadata metadata =
                new HTTPRouteResource.Metadata(
                        "{{ .Values.SERVICE_NAME }}-java-annotations-" + type.name().toLowerCase(),
                        buildRouteLabels(routeLabels)
                );

        List<HTTPRouteResource.Spec.BackendRef> backendRefs =
                List.of(serviceBackendRef(this.backendRefVal, servicePort));

        List<HTTPRouteResource.Spec.Rule> ruleList = rules.stream()
                .sorted(Comparator.comparing(PlannedRule::value, pathSpecificity())
                        .thenComparing(r -> r.matchType().equals(MATCH_TYPE_EXACT) ? 0 : 1))
                .map(rule -> toRule(rule, backendRefs))
                .toList();

        HTTPRouteResource.Spec spec = new HTTPRouteResource.Spec(getParentRefs(type), ruleList);
        return new HTTPRouteResource(metadata, spec);
    }

    static Map<String, String> buildRouteLabels(Map<String, String> customRouteLabels) {
        if (customRouteLabels == null || customRouteLabels.isEmpty()) {
            return new TreeMap<>(DEFAULT_ROUTE_LABELS);
        }
        return new TreeMap<>(customRouteLabels);
    }

    private static Set<HTTPRouteResource.Spec.ParentRef> getParentRefs(HttpRoute.Type type) {
        LinkedHashSet<HTTPRouteResource.Spec.ParentRef> parentRefs = new LinkedHashSet<>();

        switch (type) {
            case FACADE -> parentRefs.add(serviceParentRef("{{ .Values.SERVICE_NAME }}"));
            case INTERNAL -> parentRefs.add(serviceParentRef(HttpRoute.Type.INTERNAL.gatewayName()));
            case PUBLIC -> {
                parentRefs.add(gatewayParentRef(HttpRoute.Type.PUBLIC.gatewayName()));
                parentRefs.add(gatewayParentRef(HttpRoute.Type.PRIVATE.gatewayName()));
                parentRefs.add(serviceParentRef(HttpRoute.Type.INTERNAL.gatewayName()));
            }
            case PRIVATE -> {
                parentRefs.add(gatewayParentRef(HttpRoute.Type.PRIVATE.gatewayName()));
                parentRefs.add(serviceParentRef(HttpRoute.Type.INTERNAL.gatewayName()));
            }
            default -> parentRefs.add(gatewayParentRef(type.gatewayName()));
        }

        return parentRefs;
    }

    static HTTPRouteResource.Spec.ParentRef gatewayParentRef(String name) {
        return new HTTPRouteResource.Spec.ParentRef(PARENT_REF_GROUP_GATEWAY, PARENT_REF_KIND_GATEWAY, name);
    }

    static HTTPRouteResource.Spec.ParentRef serviceParentRef(String name) {
        return new HTTPRouteResource.Spec.ParentRef(PARENT_REF_GROUP_SERVICE, PARENT_REF_KIND_SERVICE, name);
    }

    private static HTTPRouteResource.Spec.BackendRef serviceBackendRef(String name, int port) {
        return new HTTPRouteResource.Spec.BackendRef(
                PARENT_REF_GROUP_SERVICE,
                PARENT_REF_KIND_SERVICE,
                name,
                port,
                1
        );
    }

    /**
     * More segments first, then longer path, then lexical order.
     */
    static Comparator<String> pathSpecificity() {
        return (left, right) -> {
            int leftSegments = pathSegmentCount(left);
            int rightSegments = pathSegmentCount(right);
            if (leftSegments != rightSegments) {
                return Integer.compare(rightSegments, leftSegments);
            }

            if (left.length() != right.length()) {
                return Integer.compare(right.length(), left.length());
            }

            return left.compareTo(right);
        };
    }

    static Comparator<HttpRoute> pathSpecificityComparator() {
        return Comparator.comparing(HttpRoute::gatewayPath, pathSpecificity());
    }

    static int pathSegmentCount(String path) {
        if (path == null || path.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String segment : path.split("/")) {
            if (!segment.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static HTTPRouteResource.Spec.Rule toRule(PlannedRule rule, List<HTTPRouteResource.Spec.BackendRef> backendRefs) {
        HTTPRouteResource.Spec.Match match = new HTTPRouteResource.Spec.Match(
                new HTTPRouteResource.Spec.Match.Path(rule.matchType(), rule.value()));

        List<HTTPRouteResource.Spec.Filter> filters = buildRewriteFilter(rule);
        HTTPRouteResource.Spec.Rule.Timeouts timeouts = buildTimeout(rule);

        return new HTTPRouteResource.Spec.Rule(
                List.of(match),
                filters,
                backendRefs,
                timeouts
        );
    }

    private static List<HTTPRouteResource.Spec.Filter> buildRewriteFilter(PlannedRule rule) {
        if (rule.rewrite() == null) {
            return List.of();
        }

        boolean exact = rule.matchType().equals(MATCH_TYPE_EXACT);
        HTTPRouteResource.Spec.Filter.UrlRewrite.Path rewritePath = exact
                ? new HTTPRouteResource.Spec.Filter.UrlRewrite.Path(FILTER_PATH_TYPE_REPLACE_FULL_PATH, null, rule.rewrite())
                : new HTTPRouteResource.Spec.Filter.UrlRewrite.Path(FILTER_PATH_TYPE_REPLACE_PREFIX_MATCH, rule.rewrite(), null);
        HTTPRouteResource.Spec.Filter.UrlRewrite urlRewrite =
                new HTTPRouteResource.Spec.Filter.UrlRewrite(rewritePath);
        HTTPRouteResource.Spec.Filter filter =
                new HTTPRouteResource.Spec.Filter(FILTER_TYPE_URL_REWRITE, urlRewrite);
        return List.of(filter);
    }

    private static HTTPRouteResource.Spec.Rule.Timeouts buildTimeout(PlannedRule rule) {
        if (rule.timeout() <= 0) {
            return null;
        }
        return new HTTPRouteResource.Spec.Rule.Timeouts(formatDuration(rule.timeout()));
    }

    static String writeYaml(Object resource) {
        try {
            return YAML_MAPPER.writeValueAsString(resource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize " + resource.getClass().getSimpleName() + " to YAML", e);
        }
    }

    private static ObjectMapper yamlMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
