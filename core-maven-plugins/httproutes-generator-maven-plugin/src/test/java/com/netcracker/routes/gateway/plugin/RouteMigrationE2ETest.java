package com.netcracker.routes.gateway.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.netcracker.routes.gateway.plugin.GenerateRoutesMojoExecuteTest.OUTPUT_FILE;
import static com.netcracker.routes.gateway.plugin.GenerateRoutesMojoExecuteTest.execute;
import static com.netcracker.routes.gateway.plugin.GenerateRoutesMojoExecuteTest.mojo;
import static com.netcracker.routes.gateway.plugin.GenerateRoutesMojoExecuteTest.set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests of the controller sets in the {@code e2e} test packages: the example of regex-routes-migration.md
 * with and without {@code @ForbiddenRoute}, and facade and composite routes.
 */
class RouteMigrationE2ETest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final String RESOURCE = "/api/v1/my-service/resource";
    private static final String ORDER = "/api/v1/my-service/order";

    @TempDir
    Path baseDir;

    private String generate(String controllerSet, boolean auto) throws Exception {
        GenerateRoutesMojo mojo = mojo(controllerSet);
        set(mojo, "autoGenerateAuthorizationPolicies", auto);
        execute(mojo, baseDir);
        return Files.readString(baseDir.resolve(OUTPUT_FILE));
    }

    /**
     * @return the YAML documents of the output file, without the header and the Helm guard
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> documents(String yaml) throws Exception {
        String body = yaml.lines().filter(l -> !l.startsWith("#") && !l.startsWith("{{-")).collect(Collectors.joining("\n"));
        List<Map<String, Object>> documents = new ArrayList<>();
        try (var values = YAML.readerFor(Map.class).readValues(body)) {
            while (values.hasNext()) {
                documents.add((Map<String, Object>) values.next());
            }
        }
        return documents;
    }

    @SuppressWarnings("unchecked")
    private static <T> T at(Object node, Object... path) {
        Object current = node;
        for (Object key : path) {
            current = key instanceof Integer i ? ((List<Object>) current).get(i) : ((Map<String, Object>) current).get(key);
        }
        return (T) current;
    }

    private static Map<String, Map<String, Object>> byName(List<Map<String, Object>> documents) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        documents.forEach(d -> result.put(at(d, "metadata", "name"), d));
        return result;
    }

    /**
     * @return the rules of an HTTPRoute, for example {@code PathPrefix /a -> ReplacePrefixMatch /b}
     */
    private static List<String> httpRouteRules(Map<String, Object> route) {
        List<Map<String, Object>> rules = at(route, "spec", "rules");
        return rules.stream().map(rule -> {
            String match = at(rule, "matches", 0, "path", "type") + " " + at(rule, "matches", 0, "path", "value");
            List<Map<String, Object>> filters = at(rule, "filters");
            if (filters.isEmpty()) {
                return match;
            }
            Map<String, Object> rewrite = at(filters.get(0), "urlRewrite", "path");
            String type = (String) rewrite.get("type");
            String value = (String) (type.equals("ReplaceFullPath") ? rewrite.get("replaceFullPath") : rewrite.get("replacePrefixMatch"));
            return match + " -> " + type + " " + value;
        }).toList();
    }

    /**
     * @return the paths and notPaths of each rule of an AuthorizationPolicy
     */
    private static List<List<List<String>>> policyRules(Map<String, Object> policy) {
        List<Map<String, Object>> rules = at(policy, "spec", "rules");
        return rules.stream().map(rule -> {
            Map<String, Object> operation = at(rule, "to", 0, "operation");
            List<String> notPaths = operation.containsKey("notPaths") ? at(operation, "notPaths") : List.of();
            return List.of(at(operation, "paths"), notPaths);
        }).toList();
    }

    private static final String NAME = "{{ .Values.SERVICE_NAME }}-java-annotations-";

    private static List<List<List<String>>> expectedOrderRule() {
        return List.of(List.of(List.of(ORDER, ORDER + "/{**}"), List.of(ORDER + "/{*}/items", ORDER + "/{*}/items/{**}")));
    }

    private static List<List<String>> expectedInternalApiRule() {
        return List.of(List.of(RESOURCE + "/{*}/internal-api", RESOURCE + "/{*}/internal-api/{**}"),
                List.of(RESOURCE + "/{*}/internal-api/status", RESOURCE + "/{*}/internal-api/status/{**}"));
    }

    // 7.1

    @Test
    void migrationDocumentExampleWithForbiddenRoutes() throws Exception {
        String yaml = generate("forbidden", false);
        List<Map<String, Object>> documents = documents(yaml);

        assertEquals(List.of("HTTPRoute", "AuthorizationPolicy", "AuthorizationPolicy", "AuthorizationPolicy"),
                documents.stream().map(d -> d.get("kind")).toList());
        assertFalse(yaml.contains("RegularExpression") || yaml.contains("VirtualService") || yaml.contains("EnvoyFilter"), yaml);

        Map<String, Map<String, Object>> byName = byName(documents);
        assertEquals(List.of(
                        "PathPrefix " + RESOURCE + " -> ReplacePrefixMatch /resource",
                        "PathPrefix " + ORDER + " -> ReplacePrefixMatch /order"),
                httpRouteRules(byName.get(NAME + "public")));
        List<List<List<String>>> publicRules = new ArrayList<>(expectedOrderRule());
        publicRules.add(expectedInternalApiRule());
        assertEquals(publicRules, policyRules(byName.get(NAME + "deny-public")));
        assertEquals(publicRules, policyRules(byName.get(NAME + "deny-private")));
        assertEquals(expectedOrderRule(), policyRules(byName.get(NAME + "deny-internal")));
    }

    @Test
    void migrationDocumentExampleWithoutForbiddenRoutesFails() throws Exception {
        MojoFailureException e = assertThrows(MojoFailureException.class, () -> generate("unforbidden", false));

        assertEquals("2 route migration errors, see log", e.getMessage());
        assertFalse(Files.exists(baseDir.resolve(OUTPUT_FILE)));
    }

    // 7.2

    @Test
    void automaticPoliciesMatchExplicitOnes() throws Exception {
        Map<String, Map<String, Object>> explicit = byName(documents(generate("forbidden", false)));
        Map<String, Map<String, Object>> automatic = byName(documents(generate("unforbidden", true)));

        assertEquals(explicit.keySet(), automatic.keySet());
        for (String gateway : List.of("public", "private", "internal")) {
            assertEquals(policyRules(explicit.get(NAME + "deny-" + gateway)), policyRules(automatic.get(NAME + "deny-" + gateway)), gateway);
        }
        assertEquals(httpRouteRules(explicit.get(NAME + "public")), httpRouteRules(automatic.get(NAME + "public")));
    }

    // 7.3

    @Test
    void facadeAndCompositeRoutesGoToServiceBoundRoute() throws Exception {
        String yaml = generate("facade", false);
        Map<String, Map<String, Object>> byName = byName(documents(yaml));

        assertEquals(List.of(NAME + "public", NAME + "facade"), List.copyOf(byName.keySet()));
        Map<String, Object> facade = byName.get(NAME + "facade");
        assertEquals(List.of(Map.of("group", "", "kind", "Service", "name", "{{ .Values.SERVICE_NAME }}")),
                at(facade, "spec", "parentRefs"));
        assertEquals(List.of(
                        "PathPrefix /api/v1/svc/shared -> ReplacePrefixMatch /shared",
                        "PathPrefix /facade/items -> ReplacePrefixMatch /items",
                        "PathPrefix /orders"),
                httpRouteRules(facade));
        assertEquals(List.of("PathPrefix /api/v1/svc/shared -> ReplacePrefixMatch /shared"), httpRouteRules(byName.get(NAME + "public")));
        assertFalse(yaml.contains("composite-gw") || yaml.contains("example.com"), yaml);
        assertFalse(yaml.contains("AuthorizationPolicy"), yaml);
    }

    @Test
    void noServiceBoundRouteWithoutRewrite() throws Exception {
        String yaml = generate("facadeidentity", false);
        Map<String, Map<String, Object>> byName = byName(documents(yaml));

        assertEquals(List.of(NAME + "public"), List.copyOf(byName.keySet()));
        assertEquals(List.of("PathPrefix /shared"), httpRouteRules(byName.get(NAME + "public")));
        assertFalse(yaml.contains("composite-gw") || yaml.contains("example.com"), yaml);
    }

    @Test
    void generatedFileIsByteIdenticalOnRerun() throws Exception {
        assertEquals(generate("forbidden", false), generate("forbidden", false));
        assertTrue(generate("facade", false).contains(NAME + "facade"));
    }
}
