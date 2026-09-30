package com.netcracker.routes.gateway.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.netcracker.routes.gateway.plugin.HttpRoute.Type.INTERNAL;
import static com.netcracker.routes.gateway.plugin.HttpRoute.Type.PRIVATE;
import static com.netcracker.routes.gateway.plugin.HttpRoute.Type.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationPolicyRendererTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final String RESOURCE = "/api/v1/svc/resource";
    private static final String ORDER = "/api/v1/svc/order";

    private static HttpRoute route(String gatewayPath, HttpRoute.Type type) {
        return new HttpRoute(gatewayPath, gatewayPath, type);
    }

    private static ForbiddenPath forbidden(String gatewayPath, HttpRoute.Type... gateways) {
        return new ForbiddenPath(gatewayPath, Set.of(gateways));
    }

    private static String render(boolean auto, Problems problems, Set<HttpRoute> routes, Set<ForbiddenPath> forbidden) {
        return new AuthorizationPolicyRenderer(Map.of(), auto).generateAuthorizationPoliciesYaml(routes, forbidden, problems);
    }

    private static String render(boolean auto, Set<HttpRoute> routes, Set<ForbiddenPath> forbidden) {
        Problems problems = new Problems();
        String yaml = render(auto, problems, routes, forbidden);
        assertEquals(List.of(), problems.errors());
        return yaml;
    }

    /**
     * @return the rules of each policy by gateway, for example {@code /a/{*} !/a/{*}/b}: the first path of the rule
     * followed by the first path of each notPaths subtree
     */
    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> rules(String yaml) throws Exception {
        Map<String, List<String>> result = new LinkedHashMap<>();
        try (var values = YAML.readerFor(Map.class).readValues(yaml)) {
            while (values.hasNext()) {
                Map<String, Object> policy = (Map<String, Object>) values.next();
                String name = (String) ((Map<String, Object>) policy.get("metadata")).get("name");
                List<String> rules = new ArrayList<>();
                for (Object rule : (List<Object>) ((Map<String, Object>) policy.get("spec")).get("rules")) {
                    Map<String, Object> operation = (Map<String, Object>) ((Map<String, Object>)
                            ((List<Object>) ((Map<String, Object>) rule).get("to")).get(0)).get("operation");
                    assertEquals(List.of("8080"), operation.get("ports"));
                    List<String> paths = (List<String>) operation.get("paths");
                    List<String> notPaths = (List<String>) operation.getOrDefault("notPaths", List.of());
                    rules.add(paths.get(0) + notPaths.stream().filter(p -> !p.endsWith("{**}")).map(p -> " !" + p)
                            .collect(Collectors.joining()));
                }
                result.put(name.substring(name.lastIndexOf('-') + 1), rules);
            }
        }
        return result;
    }

    @Test
    void forbiddenOnPublicAndPrivate() {
        String yaml = render(false, Set.of(
                        route(RESOURCE, PUBLIC),
                        route(RESOURCE + "/{id}/internal-api", INTERNAL),
                        route(RESOURCE + "/{id}/internal-api/status", PUBLIC)),
                Set.of(forbidden(RESOURCE + "/{id}/internal-api", PUBLIC, PRIVATE)));

        assertEquals("""
                ---
                apiVersion: "security.istio.io/v1"
                kind: "AuthorizationPolicy"
                metadata:
                  name: "{{ .Values.SERVICE_NAME }}-java-annotations-deny-public"
                  labels:
                    app.kubernetes.io/managed-by: "{{ .Values.MANAGED_BY }}"
                    app.kubernetes.io/name: "{{ .Values.SERVICE_NAME }}"
                    app.kubernetes.io/part-of: "{{ .Values.APPLICATION_NAME }}"
                    app.kubernetes.io/processed-by-operator: "istiod"
                    deployer.cleanup/allow: "true"
                    deployment.netcracker.com/sessionId: "{{ .Values.DEPLOYMENT_SESSION_ID }}"
                spec:
                  targetRefs:
                  - group: "gateway.networking.k8s.io"
                    kind: "Gateway"
                    name: "public-gateway"
                  action: "DENY"
                  rules:
                  - to:
                    - operation:
                        ports:
                        - "8080"
                        paths:
                        - "/api/v1/svc/resource/{*}/internal-api"
                        - "/api/v1/svc/resource/{*}/internal-api/{**}"
                        notPaths:
                        - "/api/v1/svc/resource/{*}/internal-api/status"
                        - "/api/v1/svc/resource/{*}/internal-api/status/{**}"
                ---
                apiVersion: "security.istio.io/v1"
                kind: "AuthorizationPolicy"
                metadata:
                  name: "{{ .Values.SERVICE_NAME }}-java-annotations-deny-private"
                  labels:
                    app.kubernetes.io/managed-by: "{{ .Values.MANAGED_BY }}"
                    app.kubernetes.io/name: "{{ .Values.SERVICE_NAME }}"
                    app.kubernetes.io/part-of: "{{ .Values.APPLICATION_NAME }}"
                    app.kubernetes.io/processed-by-operator: "istiod"
                    deployer.cleanup/allow: "true"
                    deployment.netcracker.com/sessionId: "{{ .Values.DEPLOYMENT_SESSION_ID }}"
                spec:
                  targetRefs:
                  - group: "gateway.networking.k8s.io"
                    kind: "Gateway"
                    name: "private-gateway"
                  action: "DENY"
                  rules:
                  - to:
                    - operation:
                        ports:
                        - "8080"
                        paths:
                        - "/api/v1/svc/resource/{*}/internal-api"
                        - "/api/v1/svc/resource/{*}/internal-api/{**}"
                        notPaths:
                        - "/api/v1/svc/resource/{*}/internal-api/status"
                        - "/api/v1/svc/resource/{*}/internal-api/status/{**}"
                """, yaml);
        assertFalse(yaml.contains("internal-gateway-service"), yaml);
    }

    @Test
    void prefixCutFromARouteWithVariables() throws Exception {
        String yaml = render(false, Set.of(route(ORDER + "/{id}/items", PUBLIC)),
                Set.of(forbidden(ORDER, PUBLIC, PRIVATE, INTERNAL)));

        List<String> rule = List.of(ORDER + " !" + ORDER + "/{*}/items");
        assertEquals(Map.of("public", rule, "private", rule, "internal", rule), rules(yaml));
        assertTrue(yaml.contains("""
                  targetRefs:
                  - group: ""
                    kind: "Service"
                    name: "internal-gateway-service"
                """), yaml);
    }

    @Test
    void missingForbiddenRouteIsAnError() {
        Problems problems = new Problems();

        String yaml = render(false, problems, Set.of(route(ORDER + "/{id}/items", PUBLIC)), Set.of(forbidden(ORDER, PUBLIC)));

        assertTrue(yaml.contains("deny-public") && !yaml.contains("deny-private"), yaml);
        assertEquals(List.of(ORDER + " is not routed by legacy, but Istio routes it by PathPrefix " + ORDER + " cut from "
                + ORDER + "/{id}/items on private-gateway, internal-gateway-service: add @ForbiddenRoute({PRIVATE, INTERNAL}) "
                + "to the element mapped to " + ORDER + ", or set autoGenerateAuthorizationPolicies to generate the DENY rules"),
                problems.errors());
    }

    @Test
    void automaticRulesMatchExplicitOnes() throws Exception {
        Set<HttpRoute> routes = Set.of(
                route(RESOURCE, PUBLIC),
                route(RESOURCE + "/{id}/internal-api", INTERNAL),
                route(RESOURCE + "/{id}/internal-api/status", PUBLIC),
                route(ORDER + "/{id}/items", PUBLIC));

        assertEquals(render(false, routes, Set.of(
                        forbidden(RESOURCE + "/{id}/internal-api", PUBLIC, PRIVATE),
                        forbidden(ORDER, PUBLIC, PRIVATE, INTERNAL))),
                render(true, routes, Set.of()));
        assertEquals(List.of(ORDER + " !" + ORDER + "/{*}/items",
                        RESOURCE + "/{*}/internal-api !" + RESOURCE + "/{*}/internal-api/status"),
                rules(render(true, routes, Set.of())).get("public"));
    }

    @Test
    void shorterRouteExposedOnTheGatewayCoversThePrefix() {
        assertEquals("", render(false, Set.of(route(ORDER, PUBLIC), route(ORDER + "/{id}/items", PUBLIC)), Set.of()));
    }

    @Test
    void narrowerRouteOutsideAnyExposedPrefixNeedsNoRule() {
        assertEquals("", render(false, Set.of(route(RESOURCE, PUBLIC), route(ORDER + "/internal", INTERNAL)), Set.of()));
    }

    @Test
    void facadeRoutesNeedNoRule() {
        assertEquals("", render(false, Set.of(route(ORDER + "/{id}", HttpRoute.Type.FACADE)), Set.of()));
    }

    @Test
    void nestedForbiddenPaths() throws Exception {
        String yaml = render(true, Set.of(
                route(RESOURCE, PUBLIC),
                route(RESOURCE + "/{id}/internal-api", INTERNAL),
                route(RESOURCE + "/{id}/internal-api/admin", PRIVATE)), Set.of());

        assertEquals(Map.of(
                        "public", List.of(RESOURCE + "/{*}/internal-api", RESOURCE + "/{*}/internal-api/admin"),
                        "private", List.of(RESOURCE + "/{*}/internal-api !" + RESOURCE + "/{*}/internal-api/admin")),
                rules(yaml));
    }

    @Test
    void literalSegmentBelowAVariableIsNotForbidden() throws Exception {
        String yaml = render(true, Set.of(route(ORDER + "/{id}/items", PUBLIC), route(ORDER + "/all/items/x", PUBLIC)), Set.of());

        assertEquals(List.of(ORDER + " !" + ORDER + "/all/items/x !" + ORDER + "/{*}/items"), rules(yaml).get("public"));
    }

    @Test
    void forbiddingAnExposedPathIsAnError() {
        Problems problems = new Problems();

        render(false, problems, Set.of(route(RESOURCE, PUBLIC)), Set.of(forbidden(RESOURCE, PUBLIC)));

        assertEquals(List.of("@ForbiddenRoute forbids " + RESOURCE + " on public-gateway, where a route with this gateway "
                + "path is exposed"), problems.errors());
    }

    @Test
    void routesLegacyMatchesAForbiddenPathByAreExcluded() throws Exception {
        assertEquals(List.of("/api/v1/svc/admin !/api/v1/svc"), rules(render(false,
                Set.of(route("/api/v1/svc", PUBLIC)), Set.of(forbidden("/api/v1/svc/admin", PUBLIC)))).get("public"));
        assertEquals(List.of("/api/{*} !/api/v1/x/y"), rules(render(false,
                Set.of(route("/api/v1/x/y", PUBLIC)), Set.of(forbidden("/api/{version}", PUBLIC)))).get("public"));
    }

    @Test
    void shorterRouteLegacyMatchesACutPrefixByIsExcluded() throws Exception {
        String order = "/api/v1/my-service/order";
        String yaml = render(true, Set.of(route(order + "/{id}/items", PUBLIC), route("/{a}/{b}/{c}/{d}/{e}", PUBLIC)), Set.of());

        assertEquals(List.of("/ !" + order + "/{*}/items !/{*}/{*}/{*}/{*}/{*}",
                        order + " !" + order + "/{*}/items !/{*}/{*}/{*}/{*}/{*}"),
                rules(yaml).get("public"));
    }

    @Test
    void pathBelowALongerForbiddenRouteKeepsItsRule() throws Exception {
        assertEquals(List.of("/api/v1/svc/admin", "/api/v1/svc/admin/users"), rules(render(false,
                Set.of(route("/api/v1/svc", PUBLIC), route("/api/v1/svc/admin", PRIVATE)),
                Set.of(forbidden("/api/v1/svc/admin", PUBLIC), forbidden("/api/v1/svc/admin/users", PUBLIC)))).get("public"));
    }

    @Test
    void partialSegmentVariableIsAnError() {
        Problems problems = new Problems();

        render(false, problems, Set.of(route("/files/{name}.txt", PUBLIC)), Set.of(forbidden("/files", PUBLIC, PRIVATE, INTERNAL)));

        assertEquals(List.of("/files/{name}.txt is a forbidden path or overlaps one, and an AuthorizationPolicy can't "
                + "express it: variables must take up whole path segments, and * wildcards aren't supported"), problems.errors());
    }

    @Test
    void customLabels() {
        String yaml = new AuthorizationPolicyRenderer(Map.of("team", "platform"), false).generateAuthorizationPoliciesYaml(
                Set.of(route(RESOURCE, PUBLIC)), Set.of(forbidden("/a", PUBLIC)), new Problems());

        assertTrue(yaml.contains("  labels:\n    team: \"platform\"\nspec:"), yaml);
        assertTrue(yaml.contains("paths:\n        - \"/a\"\n        - \"/a/{**}\"\n"), yaml);
        assertFalse(yaml.contains("notPaths"), yaml);
    }
}
