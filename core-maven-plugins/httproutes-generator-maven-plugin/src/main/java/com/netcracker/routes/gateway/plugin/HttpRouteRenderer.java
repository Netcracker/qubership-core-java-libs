package com.netcracker.routes.gateway.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Renders the planned rules (design D6, D7, D15) as one HTTPRoute per resource type, in the order public, private,
 * internal, facade.
 */
public class HttpRouteRenderer {

    private static final List<HttpRoute.Type> RESOURCE_ORDER =
            List.of(HttpRoute.Type.PUBLIC, HttpRoute.Type.PRIVATE, HttpRoute.Type.INTERNAL, HttpRoute.Type.FACADE);
    private static final String FILTER_TYPE_URL_REWRITE = "URLRewrite";

    private static final long SECOND = 1_000;
    private static final long MINUTE = 60_000;
    private static final long HOUR = 3_600_000;
    public static final String PARENT_REF_KIND_SERVICE = Gateway.PARENT_REF_KIND_SERVICE;
    public static final String PARENT_REF_KIND_GATEWAY = Gateway.PARENT_REF_KIND_GATEWAY;
    public static final String PARENT_REF_GROUP_GATEWAY = Gateway.PARENT_REF_GROUP_GATEWAY;
    public static final String PARENT_REF_GROUP_SERVICE = Gateway.PARENT_REF_GROUP_SERVICE;

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

    public String generateHttpRoutesYaml(int servicePort, List<PlannedRule> rules) {
        return createHttpRoutes(servicePort, rules).stream()
                .map(ResourceLabels::writeYaml)
                .collect(Collectors.joining());
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

    private List<HTTPRouteResource> createHttpRoutes(int servicePort, List<PlannedRule> rules) {
        List<HTTPRouteResource> resources = new ArrayList<>();
        for (HttpRoute.Type type : RESOURCE_ORDER) {
            List<PlannedRule> typeRules = rules.stream().filter(r -> r.resourceType() == type).toList();
            if (!typeRules.isEmpty()) {
                resources.add(toResource(type, typeRules, servicePort));
            }
        }
        return resources;
    }

    private HTTPRouteResource toResource(HttpRoute.Type type, List<PlannedRule> rules, int servicePort) {
        HTTPRouteResource.Metadata metadata =
                new HTTPRouteResource.Metadata(
                        "{{ .Values.SERVICE_NAME }}-java-annotations-" + type.name().toLowerCase(),
                        ResourceLabels.of(routeLabels)
                );

        List<HTTPRouteResource.Spec.BackendRef> backendRefs =
                List.of(serviceBackendRef(this.backendRefVal, servicePort));

        List<HTTPRouteResource.Spec.Rule> ruleList = rules.stream()
                .sorted(ruleOrder())
                .map(rule -> toRule(rule, backendRefs))
                .toList();

        HTTPRouteResource.Spec spec = new HTTPRouteResource.Spec(getParentRefs(type), ruleList);
        return new HTTPRouteResource(metadata, spec);
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

    private static HTTPRouteResource.Spec.ParentRef gatewayParentRef(String name) {
        return new HTTPRouteResource.Spec.ParentRef(PARENT_REF_GROUP_GATEWAY, PARENT_REF_KIND_GATEWAY, name);
    }

    private static HTTPRouteResource.Spec.ParentRef serviceParentRef(String name) {
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

    /**
     * Path specificity of the match value, and {@code Exact} before {@code PathPrefix} for the same value.
     */
    static Comparator<PlannedRule> ruleOrder() {
        return Comparator.comparing(PlannedRule::value, pathSpecificity())
                .thenComparing(PlannedRule::matchType);
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
                new HTTPRouteResource.Spec.Match.Path(rule.matchType().apiName(), rule.value()));

        return new HTTPRouteResource.Spec.Rule(
                List.of(match),
                buildRewriteFilter(rule),
                backendRefs,
                buildTimeout(rule)
        );
    }

    private static List<HTTPRouteResource.Spec.Filter> buildRewriteFilter(PlannedRule rule) {
        if (!rule.hasRewrite()) {
            return List.of();
        }
        boolean fullPath = rule.rewriteType() == PlannedRule.RewriteType.REPLACE_FULL_PATH;
        HTTPRouteResource.Spec.Filter.UrlRewrite.Path rewritePath = new HTTPRouteResource.Spec.Filter.UrlRewrite.Path(
                rule.rewriteType().apiName(),
                fullPath ? null : rule.rewriteValue(),
                fullPath ? rule.rewriteValue() : null);
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
}
