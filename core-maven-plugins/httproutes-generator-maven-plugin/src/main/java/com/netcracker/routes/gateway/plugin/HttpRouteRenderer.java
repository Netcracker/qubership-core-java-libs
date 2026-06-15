package com.netcracker.routes.gateway.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.*;
import java.util.stream.Collectors;

public class HttpRouteRenderer {

    private static final ObjectMapper YAML_MAPPER = yamlMapper();
    private static final String MATCH_TYPE_PATH_PREFIX = "PathPrefix";
    private static final String MATCH_TYPE_REGULAR_EXPRESSION = "RegularExpression";
    private static final String FILTER_TYPE_URL_REWRITE = "URLRewrite";
    private static final String FILTER_PATH_TYPE_REPLACE_PREFIX_MATCH = "ReplacePrefixMatch";
    private static final String REGEX_MANUAL_REVIEW_WARNING = """
            # MANUAL REVIEW REQUIRED
            # RegularExpression path matches may conflict with sibling rules with Prefix matches.
            # Also, replacePrefixMatch only works with PathPrefix matches and cannot be
            # used with RegularExpression matches. Test regex matching routes carefully.
            """;

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
                    public record Path(String type, String replacePrefixMatch) {
                    }
                }
            }

            public record BackendRef(String group, String kind, String name, Integer port, Integer weight) {
            }
        }
    }

    public String generateHttpRoutesYaml(int servicePort, Set<HttpRoute> httpRoutes) {
        List<HTTPRouteResource> routes = createHttpRoutes(servicePort, httpRoutes);

        return routes.stream()
                .map(HttpRouteRenderer::renderValidatedYaml)
                .collect(Collectors.joining());
    }

    private static HTTPRouteResource.Spec.Match.Path convertSpringPathToHttpRoutePath(String springPath) {
        if (springPath == null || springPath.isEmpty()) {
            return new HTTPRouteResource.Spec.Match.Path(MATCH_TYPE_PATH_PREFIX, "/");
        }

        if (springPath.contains("{")) {
            return new HTTPRouteResource.Spec.Match.Path(MATCH_TYPE_REGULAR_EXPRESSION, normalizePath(springPath.replaceAll("\\{([^/]+?)}", "([^/]+)")));
        } else {
            return new HTTPRouteResource.Spec.Match.Path(MATCH_TYPE_PATH_PREFIX, normalizePath(springPath));
        }
    }

    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        if (!path.startsWith("/")) {
            return "/" + path;
        }
        return path;
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

    private List<HTTPRouteResource> createHttpRoutes(int servicePort, Set<HttpRoute> httpRoutes) {
        Map<HttpRoute.Type, List<HttpRoute>> routesByType = httpRoutes
                .stream()
                .collect(Collectors.groupingBy(HttpRoute::type));

        return routesByType.entrySet().stream()
                .map(entry -> toResource(entry.getKey(), entry.getValue(), servicePort))
                .filter(Objects::nonNull)
                .toList();
    }

    private HTTPRouteResource toResource(HttpRoute.Type type, List<HttpRoute> routes, int servicePort) {
        HTTPRouteResource.Metadata metadata =
                new HTTPRouteResource.Metadata(
                        "{{ .Values.SERVICE_NAME }}-java-annotations-" + type.name().toLowerCase(),
                        buildRouteLabels(routeLabels)
                );

        List<HTTPRouteResource.Spec.BackendRef> backendRefs =
                List.of(serviceBackendRef(this.backendRefVal, servicePort));

        List<HttpRoute> sortedRoutes = routes.stream()
                .sorted(pathSpecificityComparator())
                .toList();

        List<HTTPRouteResource.Spec.Rule> ruleList = new ArrayList<>(sortedRoutes.size());
        for (HttpRoute route : sortedRoutes) {
            if (route.type() == HttpRoute.Type.FACADE && route.path().equals(route.gatewayPath())) {
                continue;
            }
            ruleList.add(toRule(route, backendRefs));
        }

        if (ruleList.isEmpty()) {
            return null;
        }

        HTTPRouteResource.Spec spec = new HTTPRouteResource.Spec(getParentRefs(type), ruleList);
        return new HTTPRouteResource(metadata, spec);
    }

    private static Map<String, String> buildRouteLabels(Map<String, String> customRouteLabels) {
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

    static Comparator<HttpRoute> pathSpecificityComparator() {
        return (left, right) -> {
            int leftSegments = pathSegmentCount(left.gatewayPath());
            int rightSegments = pathSegmentCount(right.gatewayPath());
            if (leftSegments != rightSegments) {
                return Integer.compare(rightSegments, leftSegments);
            }

            int leftLength = left.gatewayPath().length();
            int rightLength = right.gatewayPath().length();
            if (leftLength != rightLength) {
                return Integer.compare(rightLength, leftLength);
            }

            return left.gatewayPath().compareTo(right.gatewayPath());
        };
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

    private static HTTPRouteResource.Spec.Rule toRule(HttpRoute route, List<HTTPRouteResource.Spec.BackendRef> backendRefs) {
        HTTPRouteResource.Spec.Match match = new HTTPRouteResource.Spec.Match(
                convertSpringPathToHttpRoutePath(route.gatewayPath()));

        List<HTTPRouteResource.Spec.Filter> filters = buildRewriteFilter(route);
        HTTPRouteResource.Spec.Rule.Timeouts timeouts = buildTimeout(route);

        return new HTTPRouteResource.Spec.Rule(
                List.of(match),
                filters,
                backendRefs,
                timeouts
        );
    }

    private static List<HTTPRouteResource.Spec.Filter> buildRewriteFilter(HttpRoute route) {
        String normalizedGateway = normalizePath(route.gatewayPath());
        String normalizedService = normalizePath(route.path());
        if (normalizedGateway.equals(normalizedService)) {
            return List.of();
        }

        HTTPRouteResource.Spec.Filter.UrlRewrite.Path rewritePath =
                new HTTPRouteResource.Spec.Filter.UrlRewrite.Path(FILTER_PATH_TYPE_REPLACE_PREFIX_MATCH, normalizedService);
        HTTPRouteResource.Spec.Filter.UrlRewrite urlRewrite =
                new HTTPRouteResource.Spec.Filter.UrlRewrite(rewritePath);
        HTTPRouteResource.Spec.Filter filter =
                new HTTPRouteResource.Spec.Filter(FILTER_TYPE_URL_REWRITE, urlRewrite);
        return List.of(filter);
    }

    private static HTTPRouteResource.Spec.Rule.Timeouts buildTimeout(HttpRoute route) {
        if (route.timeout() <= 0) {
            return null;
        }
        return new HTTPRouteResource.Spec.Rule.Timeouts(formatDuration(route.timeout()));
    }

    private static String renderValidatedYaml(HTTPRouteResource route) {
        String renderedYaml = writeYaml(route);
        return hasRegularExpressionMatch(route) ? REGEX_MANUAL_REVIEW_WARNING + renderedYaml : renderedYaml;
    }

    private static boolean hasRegularExpressionMatch(HTTPRouteResource route) {
        if (route == null || route.spec() == null || route.spec().rules() == null) {
            return false;
        }
        return route.spec().rules().stream()
                .filter(Objects::nonNull)
                .map(HTTPRouteResource.Spec.Rule::matches)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(HTTPRouteResource.Spec.Match::path)
                .filter(Objects::nonNull)
                .anyMatch(path -> MATCH_TYPE_REGULAR_EXPRESSION.equals(path.type()));
    }

    private static String writeYaml(HTTPRouteResource route) {
        try {
            return YAML_MAPPER.writeValueAsString(route);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize HTTPRoute Resource to YAML", e);
        }
    }

    private static ObjectMapper yamlMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
