package com.netcracker.routes.gateway.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.Map;
import java.util.TreeMap;

/**
 * Labels and YAML serialization shared by the generated HTTPRoutes and AuthorizationPolicies.
 */
final class ResourceLabels {

    private static final ObjectMapper YAML_MAPPER = yamlMapper();
    private static final Map<String, String> DEFAULT_LABELS = Map.of(
            "app.kubernetes.io/name", "{{ .Values.SERVICE_NAME }}",
            "app.kubernetes.io/part-of", "{{ .Values.APPLICATION_NAME }}",
            "app.kubernetes.io/managed-by", "{{ .Values.MANAGED_BY }}",
            "deployment.netcracker.com/sessionId", "{{ .Values.DEPLOYMENT_SESSION_ID }}",
            "deployer.cleanup/allow", "true",
            "app.kubernetes.io/processed-by-operator", "istiod"
    );

    private ResourceLabels() {
    }

    /**
     * @return the custom labels if any are configured, else the default labels, sorted by key
     */
    static Map<String, String> of(Map<String, String> customLabels) {
        if (customLabels == null || customLabels.isEmpty()) {
            return new TreeMap<>(DEFAULT_LABELS);
        }
        return new TreeMap<>(customLabels);
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
