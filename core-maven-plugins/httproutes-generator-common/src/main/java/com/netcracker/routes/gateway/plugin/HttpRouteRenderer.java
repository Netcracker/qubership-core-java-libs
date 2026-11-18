package com.netcracker.routes.gateway.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HttpRouteRenderer {

    public static final String GATEWAY_PARENT_REFS_PLACEHOLDER = "__HTTPROUTES_GENERATOR_GATEWAYS_PLACEHOLDER__";

    static final String HTTPROUTES_GENERATOR_GATEWAYS_SNIPPET = String.join("\n", List.of(
            "{{- $gateways := .Values.HTTPROUTES_GENERATOR_GATEWAYS | default (list \"public-gateway\" \"private-gateway\" \"internal-gateway-service\") }}",
            "{{- if kindIs \"string\" $gateways }}",
            "{{- $gateways = splitList \",\" $gateways }}",
            "{{- end }}",
            "{{- range $gateways }}",
            "{{- $name := trim . }}",
            "{{- if $name }}",
            "  - group: {{ if eq $name \"internal-gateway-service\" }}\"\"{{ else }}gateway.networking.k8s.io{{ end }}",
            "    kind: {{ if eq $name \"internal-gateway-service\" }}Service{{ else }}Gateway{{ end }}",
            "    name: {{ $name }}",
            "{{- end }}",
            "{{- end }}"
    ));

    private static final String API_VERSION = "gateway.networking.k8s.io/v1";
    private static final String KIND_HTTP_ROUTE = "HTTPRoute";
    private static final String MATCH_TYPE_PATH_PREFIX = "PathPrefix";
    private static final String FILTER_TYPE_URL_REWRITE = "URLRewrite";
    private static final String FILTER_PATH_TYPE_REPLACE_PREFIX_MATCH = "ReplacePrefixMatch";
    private static final String GROUP_SERVICE = "";
    private static final String GROUP_GATEWAY = "gateway.networking.k8s.io";
    private static final String KIND_SERVICE = "Service";
    private static final String KIND_GATEWAY = "Gateway";

    private static final long SECOND = 1_000;
    private static final long MINUTE = 60_000;
    private static final long HOUR = 3_600_000;

    private static final Map<String, String> DEFAULT_LABELS = Map.of(
            "app.kubernetes.io/name", "{{ .Values.SERVICE_NAME }}",
            "app.kubernetes.io/part-of", "{{ .Values.APPLICATION_NAME }}",
            "app.kubernetes.io/managed-by", "{{ .Values.MANAGED_BY }}",
            "deployment.netcracker.com/sessionId", "{{ .Values.DEPLOYMENT_SESSION_ID }}",
            "deployer.cleanup/allow", "true",
            "app.kubernetes.io/processed-by-operator", "istiod"
    );

    private static final ObjectMapper YAML_MAPPER = yamlMapper();

    private final String backendRefVal;
    private final int servicePort;
    private final Map<String, String> labels;

    public HttpRouteRenderer(String backendRefVal, int servicePort, Map<String, String> labels) {
        this.backendRefVal = backendRefVal;
        this.servicePort = servicePort;
        this.labels = labels == null || labels.isEmpty() ? DEFAULT_LABELS : labels;
    }

    public String render(List<Rule> gatewayRoots, List<Rule> facadeRoots) {
        StringBuilder result = new StringBuilder();
        if (!gatewayRoots.isEmpty()) {
            append(result, renderGatewayResource(gatewayRoots));
        }
        if (!facadeRoots.isEmpty()) {
            append(result, renderFacadeResource(facadeRoots));
        }
        return result.toString();
    }

    private void append(StringBuilder result, String document) {
        if (!result.isEmpty()) {
            result.append("---\n");
        }
        result.append(document);
    }

    private String renderGatewayResource(List<Rule> roots) {
        Resource resource = new Resource(
                API_VERSION,
                KIND_HTTP_ROUTE,
                new Meta("{{ .Values.SERVICE_NAME }}-java-annotations", new TreeMap<>(labels)),
                new Spec(GATEWAY_PARENT_REFS_PLACEHOLDER, toHttpRouteRules(roots)));
        String yaml = writeYaml(resource);
        return yaml.replace("parentRefs: " + GATEWAY_PARENT_REFS_PLACEHOLDER,
                "parentRefs:\n" + HTTPROUTES_GENERATOR_GATEWAYS_SNIPPET);
    }

    private String renderFacadeResource(List<Rule> roots) {
        List<ParentRef> parentRefs = List.of(new ParentRef(GROUP_SERVICE, KIND_SERVICE, "{{ .Values.SERVICE_NAME }}"));
        Resource resource = new Resource(
                API_VERSION,
                KIND_HTTP_ROUTE,
                new Meta("{{ .Values.SERVICE_NAME }}-java-annotations-facade", new TreeMap<>(labels)),
                new Spec(parentRefs, toHttpRouteRules(roots)));
        return writeYaml(resource);
    }

    private List<HttpRouteRule> toHttpRouteRules(List<Rule> roots) {
        List<Backend> backendRefs = List.of(new Backend(GROUP_SERVICE, KIND_SERVICE, backendRefVal, servicePort, 1));
        return roots.stream()
                .sorted(java.util.Comparator.comparing(Rule::match))
                .map(root -> toHttpRouteRule(root, backendRefs))
                .toList();
    }

    private HttpRouteRule toHttpRouteRule(Rule root, List<Backend> backendRefs) {
        Match match = new Match(new PathMatch(MATCH_TYPE_PATH_PREFIX, root.match()));
        List<Filter> filters = root.match().equals(root.target()) ? List.of() : List.of(rewriteFilter(root.target()));
        Timeouts timeouts = root.timeout() > 0 ? new Timeouts(formatDuration(root.timeout())) : null;
        return new HttpRouteRule(List.of(match), filters, backendRefs, timeouts);
    }

    private Filter rewriteFilter(String target) {
        return new Filter(FILTER_TYPE_URL_REWRITE,
                new UrlRewrite(new ReplacePrefixMatchPath(FILTER_PATH_TYPE_REPLACE_PREFIX_MATCH, target)));
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

    private static String writeYaml(Resource resource) {
        try {
            return YAML_MAPPER.writeValueAsString(resource);
        } catch (Exception e) {
            throw new RouteGenerationException("Failed to serialize HTTPRoute resource to YAML: " + e.getMessage());
        }
    }

    private static ObjectMapper yamlMapper() {
        YAMLFactory factory = YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .build();
        ObjectMapper mapper = new ObjectMapper(factory);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    public record Resource(String apiVersion, String kind, Meta metadata, Spec spec) {}

    public record Meta(String name, Map<String, String> labels) {}

    public record Spec(Object parentRefs, List<HttpRouteRule> rules) {}

    public record ParentRef(String group, String kind, String name) {}

    public record HttpRouteRule(List<Match> matches, @JsonInclude(JsonInclude.Include.NON_EMPTY) List<Filter> filters,
                                 List<Backend> backendRefs, Timeouts timeouts) {}

    public record Match(PathMatch path) {}

    public record PathMatch(String type, String value) {}

    public record Filter(String type, UrlRewrite urlRewrite) {}

    public record UrlRewrite(ReplacePrefixMatchPath path) {}

    public record ReplacePrefixMatchPath(String type, String replacePrefixMatch) {}

    public record Backend(String group, String kind, String name, int port, int weight) {}

    public record Timeouts(String request) {}
}
