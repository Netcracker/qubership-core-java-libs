package com.netcracker.routes.gateway.plugin;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRouteRendererTest {

    static HttpRoute route(String gatewayPath, String servicePath, HttpRoute.Type type, long timeout) {
        return new HttpRoute(servicePath, gatewayPath, type, timeout);
    }

    static HttpRoute route(String gatewayPath, String servicePath, HttpRoute.Type type) {
        return route(gatewayPath, servicePath, type, 0);
    }

    private static String render(Problems problems, HttpRoute... routes) {
        return new HttpRouteRenderer("{{ CustomBackendRef }}").generateHttpRoutesYaml(8081, Set.of(routes), problems);
    }

    private static String render(HttpRoute... routes) {
        Problems problems = new Problems();
        String yaml = render(problems, routes);
        assertEquals(List.of(), problems.errors());
        return yaml;
    }

    /**
     * @return the rules of the rendered HTTPRoutes, for example {@code public: PathPrefix /a -> /b 5s}
     */
    private static List<String> rules(String yaml) {
        List<String> rules = new ArrayList<>();
        String resource = null;
        String rule = null;
        for (String line : yaml.lines().map(String::trim).toList()) {
            if (line.startsWith("name: \"{{ .Values.SERVICE_NAME }}-java-annotations-")) {
                resource = line.substring(line.lastIndexOf('-') + 1, line.length() - 1);
            } else if (line.startsWith("type: \"PathPrefix\"")) {
                rule = resource + ": " + line.substring(7, line.length() - 1);
            } else if (line.startsWith("value: ")) {
                rule += " " + line.substring(8, line.length() - 1);
                rules.add(rule);
            } else if (line.startsWith("replacePrefixMatch: ")) {
                rules.set(rules.size() - 1, rule + " -> " + line.substring(line.indexOf('"') + 1, line.length() - 1));
            } else if (line.startsWith("request: ")) {
                rules.set(rules.size() - 1, rules.get(rules.size() - 1) + " " + line.substring(10, line.length() - 1));
            }
        }
        return rules;
    }

    @Test
    void generatesYamlWithMatchesRewritesAndTimeouts() {
        String yaml = render(
                route("/api", "/api", HttpRoute.Type.INTERNAL),
                route("/gateway", "/svc", HttpRoute.Type.PUBLIC, 5_000),
                route("/items/{id}", "/items/{id}", HttpRoute.Type.PRIVATE));

        assertTrue(yaml.contains("HTTPRoute"));
        assertTrue(yaml.contains("ReplacePrefixMatch"));
        assertTrue(yaml.contains("request: \"5s\""));
        assertTrue(yaml.contains("type: \"PathPrefix\"\n        value: \"/items\"\n"), yaml);
        assertFalse(yaml.contains("RegularExpression"));
        assertTrue(yaml.contains("{{ CustomBackendRef }}"));
        assertFalse(yaml.contains("MANUAL REVIEW"));
    }

    @Test
    void replacesDefaultLabelsWhenCustomLabelsProvided() {
        String yaml = new HttpRouteRenderer(
                "{{ CustomBackendRef }}",
                Map.of(
                        "team", "platform",
                        "app.kubernetes.io/managed-by", "custom-manager"
                )
        ).generateHttpRoutesYaml(8081, Set.of(route("/api", "/api", HttpRoute.Type.INTERNAL)), new Problems());

        assertTrue(yaml.contains("team:"));
        assertTrue(yaml.contains("platform"));
        assertTrue(yaml.contains("app.kubernetes.io/managed-by:"));
        assertTrue(yaml.contains("custom-manager"));
        assertFalse(yaml.contains("app.kubernetes.io/name:"));
        assertFalse(yaml.contains("deployment.netcracker.com/sessionId:"));
    }

    @Test
    void rendersOnePathPrefixRuleForRoutesCutToOnePrefix() {
        String yaml = render(
                route("/api/v1/svc/items", "/items", HttpRoute.Type.INTERNAL),
                route("/api/v1/svc/items/{id}", "/items/{id}", HttpRoute.Type.INTERNAL));

        assertEquals("""
                apiVersion: "gateway.networking.k8s.io/v1"
                kind: "HTTPRoute"
                metadata:
                  name: "{{ .Values.SERVICE_NAME }}-java-annotations-internal"
                  labels:
                    app.kubernetes.io/managed-by: "{{ .Values.MANAGED_BY }}"
                    app.kubernetes.io/name: "{{ .Values.SERVICE_NAME }}"
                    app.kubernetes.io/part-of: "{{ .Values.APPLICATION_NAME }}"
                    app.kubernetes.io/processed-by-operator: "istiod"
                    deployer.cleanup/allow: "true"
                    deployment.netcracker.com/sessionId: "{{ .Values.DEPLOYMENT_SESSION_ID }}"
                spec:
                  parentRefs:
                  - group: ""
                    kind: "Service"
                    name: "internal-gateway-service"
                  rules:
                  - matches:
                    - path:
                        type: "PathPrefix"
                        value: "/api/v1/svc/items"
                    filters:
                    - type: "URLRewrite"
                      urlRewrite:
                        path:
                          type: "ReplacePrefixMatch"
                          replacePrefixMatch: "/items"
                    backendRefs:
                    - group: ""
                      kind: "Service"
                      name: "{{ CustomBackendRef }}"
                      port: 8081
                      weight: 1
                """, yaml.replace("---\n", ""));
    }

    @Test
    void rendersResourcesInOrderPublicPrivateInternalFacade() {
        String yaml = render(
                route("/facade/items", "/items", HttpRoute.Type.FACADE),
                route("/api/internal", "/api/internal", HttpRoute.Type.INTERNAL),
                route("/api/private", "/api/private", HttpRoute.Type.PRIVATE),
                route("/api/public", "/api/public", HttpRoute.Type.PUBLIC));

        int publicAt = yaml.indexOf("-java-annotations-public\"");
        int privateAt = yaml.indexOf("-java-annotations-private\"");
        int internalAt = yaml.indexOf("-java-annotations-internal\"");
        int facadeAt = yaml.indexOf("-java-annotations-facade\"");
        assertTrue(0 <= publicAt && publicAt < privateAt && privateAt < internalAt && internalAt < facadeAt, yaml);
    }

    @Test
    void serviceBoundRouteHasAllFacadeRulesAndOnlyTheServiceParentRef() {
        String yaml = render(
                route("/facade/items", "/items", HttpRoute.Type.FACADE),
                route("/orders", "/orders", HttpRoute.Type.FACADE));

        assertTrue(yaml.startsWith("---\napiVersion: \"gateway.networking.k8s.io/v1\"\nkind: \"HTTPRoute\"\nmetadata:\n"
                + "  name: \"{{ .Values.SERVICE_NAME }}-java-annotations-facade\"\n"), yaml);
        assertTrue(yaml.contains("""
                  parentRefs:
                  - group: ""
                    kind: "Service"
                    name: "{{ .Values.SERVICE_NAME }}"
                  rules:
                """), yaml);
        assertEquals(1, yaml.split("kind: \"HTTPRoute\"", -1).length - 1, yaml);
        assertTrue(yaml.contains("value: \"/facade/items\"") && yaml.contains("replacePrefixMatch: \"/items\""), yaml);
        assertTrue(yaml.contains("value: \"/orders\"\n    filters: []\n"), yaml);
    }

    @Test
    void noServiceBoundRouteWithoutRewrite() {
        String yaml = render(
                route("/items", "/items", HttpRoute.Type.FACADE),
                route("/api/public", "/api/public", HttpRoute.Type.PUBLIC));

        assertFalse(yaml.contains("-facade"), yaml);
        assertTrue(yaml.contains("-java-annotations-public"), yaml);
    }

    @Test
    void renderingTwiceGivesIdenticalOutput() {
        HttpRoute[] routes = {
                route("/facade/items", "/items", HttpRoute.Type.FACADE),
                route("/api/v1/svc/items", "/v1/items", HttpRoute.Type.PUBLIC),
                route("/api/v1/svc/items/{id}", "/v2/items/{id}", HttpRoute.Type.PUBLIC),
                route("/api/v1/svc/orders/{id}", "/orders/{id}", HttpRoute.Type.INTERNAL, 5_000),
                route("/api/v1/svc/admin", "/admin", HttpRoute.Type.PRIVATE)};

        assertEquals(render(routes), render(routes));
    }

    @Test
    void sortsRulesByPathSpecificity() {
        List<String> paths = new ArrayList<>(List.of("/alpha", "/alpha/bravo", "/alpha/bravo/charlie"));

        paths.sort(HttpRouteRenderer.pathSpecificity());

        assertEquals(List.of("/alpha/bravo/charlie", "/alpha/bravo", "/alpha"), paths);
    }

    @Test
    void sortsByLengthThenLexicalWhenSegmentsEqual() {
        List<String> paths = new ArrayList<>(List.of("/aa/za", "/aa/ab", "/aa/abcd"));

        paths.sort(HttpRouteRenderer.pathSpecificity());

        assertEquals(List.of("/aa/abcd", "/aa/ab", "/aa/za"), paths);
    }

    @Test
    void cutsGatewayAndServicePathsAtTheFirstVariable() {
        assertEquals(List.of(
                        "public: PathPrefix /api",
                        "private: PathPrefix /api/v1/svc/items -> /items",
                        "private: PathPrefix /api/v1/svc -> /svc",
                        "internal: PathPrefix /files",
                        "internal: PathPrefix /"),
                rules(render(
                        route("/api/{version}/svc/items", "/api/{version}/svc/items", HttpRoute.Type.PUBLIC),
                        route("/api/v1/svc/items/{id}/details", "/items/{id}/details", HttpRoute.Type.PRIVATE),
                        route("/api/v1/svc/{tenant}/x", "/svc/{tenant}/x", HttpRoute.Type.PRIVATE),
                        route("/files/{name}.txt", "/files/{name}.txt", HttpRoute.Type.INTERNAL),
                        route("/{id}", "/{id}", HttpRoute.Type.INTERNAL))));
    }

    @Test
    void mergesRoutesCutToOnePrefixIntoTheWidestResource() {
        Problems problems = new Problems();

        String yaml = render(problems,
                route("/api/v1/svc/resource", "/resource", HttpRoute.Type.PUBLIC),
                route("/api/v1/svc/resource/{id}", "/resource/{id}", HttpRoute.Type.PRIVATE, 5_000),
                route("/api/v1/svc/resource/{id}/internal-api/", "/resource/{id}/internal-api/", HttpRoute.Type.INTERNAL, 10_000),
                route("/api/v1/svc/order/", "/order", HttpRoute.Type.INTERNAL),
                route("/api/v1/svc/order/{id}", "/order/{id}", HttpRoute.Type.INTERNAL));

        assertEquals(List.of(
                        "public: PathPrefix /api/v1/svc/resource -> /resource",
                        "internal: PathPrefix /api/v1/svc/order -> /order"),
                rules(yaml));
        assertEquals(List.of(), problems.errors());
        assertEquals(List.of("Routes /api/v1/svc/resource (PUBLIC, ReplacePrefixMatch /resource), "
                + "/api/v1/svc/resource/{id} (PRIVATE, ReplacePrefixMatch /resource, timeout 5s), "
                + "/api/v1/svc/resource/{id}/internal-api/ (INTERNAL, ReplacePrefixMatch /resource, timeout 10s) "
                + "are merged into one rule PathPrefix /api/v1/svc/resource with the largest timeout, the 2m default"),
                problems.warnings());
    }

    @Test
    void largestExplicitTimeoutWinsWhenNoRouteHasTheDefault() {
        Problems problems = new Problems();

        String yaml = render(problems,
                route("/a", "/b", HttpRoute.Type.PUBLIC, 5_000),
                route("/a/{id}", "/b/{id}", HttpRoute.Type.PUBLIC, 10_000));

        assertEquals(List.of("public: PathPrefix /a -> /b 10s"), rules(yaml));
        assertEquals(List.of("Routes /a (PUBLIC, ReplacePrefixMatch /b, timeout 5s), /a/{id} (PUBLIC, ReplacePrefixMatch /b, "
                + "timeout 10s) are merged into one rule PathPrefix /a with the largest timeout 10s"), problems.warnings());
    }

    @Test
    void explicitTimeoutFromTheDefaultUpWinsOverTheDefault() {
        assertEquals(List.of("public: PathPrefix /a -> /b 5m"), rules(render(
                route("/a", "/b", HttpRoute.Type.PUBLIC),
                route("/a/{id}", "/b/{id}", HttpRoute.Type.PUBLIC, 300_000))));
        assertEquals(List.of("public: PathPrefix /a -> /b 2m"), rules(render(
                route("/a", "/b", HttpRoute.Type.PUBLIC),
                route("/a/{id}", "/b/{id}", HttpRoute.Type.PUBLIC, 120_000))));
    }

    @Test
    void differentRewritesForOnePrefixAreNotChecked() {
        Problems problems = new Problems();

        String yaml = render(problems,
                route("/api/v1/svc/items/{id}", "/v1/items/{id}", HttpRoute.Type.PUBLIC),
                route("/api/v1/svc/items/{id}/details", "/v2/items/{id}/details", HttpRoute.Type.INTERNAL));

        assertEquals(List.of("public: PathPrefix /api/v1/svc/items -> /v1/items"), rules(yaml));
        assertEquals(List.of(), problems.errors());
    }

    @Test
    void facadeTimeoutsWithoutRewriteAreAWarning() {
        Problems problems = new Problems();

        String yaml = render(problems, route("/items", "/items", HttpRoute.Type.FACADE, 5_000));

        assertEquals("", yaml);
        assertEquals(List.of("No facade or composite route has a rewrite, so no service-bound HTTPRoute is generated, "
                + "and the timeouts of the facade and composite routes are not applied"), problems.warnings());
    }
}
