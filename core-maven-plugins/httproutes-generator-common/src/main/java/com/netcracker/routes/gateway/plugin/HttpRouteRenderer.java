package com.netcracker.routes.gateway.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HttpRouteRenderer {

    public static final int MAX_RULES_PER_ROUTE = 16;
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

    private static final Map<String, String> DEFAULT_LABELS = Map.of(
            "app.kubernetes.io/name", "{{ .Values.SERVICE_NAME }}",
            "app.kubernetes.io/part-of", "{{ .Values.APPLICATION_NAME }}",
            "app.kubernetes.io/managed-by", "{{ .Values.MANAGED_BY }}",
            "deployment.netcracker.com/sessionId", "{{ .Values.DEPLOYMENT_SESSION_ID }}",
            "deployer.cleanup/allow", "true",
            "app.kubernetes.io/processed-by-operator", "istiod"
    );

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(YAMLFactory.builder()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .build())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

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
        renderGroup(result, gatewayRoots, "{{ .Values.SERVICE_NAME }}-java-annotations", GATEWAY_PARENT_REFS_PLACEHOLDER, true);
        renderGroup(result, facadeRoots, "{{ .Values.SERVICE_NAME }}-java-annotations-facade",
                List.of(orderedMap("group", "", "kind", "Service", "name", "{{ .Values.SERVICE_NAME }}")), false);
        return result.toString();
    }

    private void renderGroup(StringBuilder result, List<Rule> roots, String baseName, Object parentRefs, boolean gatewayParentRefs) {
        if (roots.isEmpty()) {
            return;
        }
        List<Rule> sorted = roots.stream().sorted(Comparator.comparing(Rule::match)).toList();
        List<Map<String, Object>> backendRefs = List.of(
                orderedMap("group", "", "kind", "Service", "name", backendRefVal, "port", servicePort, "weight", 1));
        for (int offset = 0, part = 1; offset < sorted.size(); offset += MAX_RULES_PER_ROUTE, part++) {
            List<Map<String, Object>> rules = sorted.subList(offset, Math.min(offset + MAX_RULES_PER_ROUTE, sorted.size()))
                    .stream()
                    .map(root -> toHttpRouteRule(root, backendRefs))
                    .toList();
            String name = part == 1 ? baseName : baseName + "-" + part;
            Map<String, Object> resource = orderedMap(
                    "apiVersion", "gateway.networking.k8s.io/v1",
                    "kind", "HTTPRoute",
                    "metadata", orderedMap("name", name, "labels", new TreeMap<>(labels)),
                    "spec", orderedMap("parentRefs", parentRefs, "rules", rules));
            String yaml = writeYaml(resource);
            if (gatewayParentRefs) {
                yaml = yaml.replace("parentRefs: " + GATEWAY_PARENT_REFS_PLACEHOLDER,
                        "parentRefs:\n" + HTTPROUTES_GENERATOR_GATEWAYS_SNIPPET);
            }
            if (!result.isEmpty()) {
                result.append("---\n");
            }
            result.append(yaml);
        }
    }

    private Map<String, Object> toHttpRouteRule(Rule root, List<Map<String, Object>> backendRefs) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("matches", List.of(Map.of("path", orderedMap("type", "PathPrefix", "value", root.match()))));
        if (!root.match().equals(root.target())) {
            rule.put("filters", List.of(orderedMap(
                    "type", "URLRewrite",
                    "urlRewrite", Map.of("path", orderedMap(
                            "type", "ReplacePrefixMatch",
                            "replacePrefixMatch", root.target())))));
        }
        rule.put("backendRefs", backendRefs);
        if (root.timeout() > 0) {
            rule.put("timeouts", Map.of("request", formatDuration(root.timeout())));
        }
        return rule;
    }

    public static String formatDuration(long ms) {
        if (ms % 3_600_000 == 0) return (ms / 3_600_000) + "h";
        if (ms % 60_000 == 0) return (ms / 60_000) + "m";
        if (ms % 1_000 == 0) return (ms / 1_000) + "s";
        return ms + "ms";
    }

    private static String writeYaml(Map<String, Object> resource) {
        try {
            return YAML_MAPPER.writeValueAsString(resource);
        } catch (Exception e) {
            throw new RouteGenerationException("Failed to serialize HTTPRoute resource to YAML: " + e.getMessage());
        }
    }

    private static Map<String, Object> orderedMap(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>(kv.length / 2);
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }
}
