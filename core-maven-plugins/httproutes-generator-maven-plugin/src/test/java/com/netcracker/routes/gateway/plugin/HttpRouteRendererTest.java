package com.netcracker.routes.gateway.plugin;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.netcracker.routes.gateway.plugin.LegacyRouteTableTest.route;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRouteRendererTest {

    static List<PlannedRule> plan(DeclaredRoute... routes) {
        return RouteMigration.run(new RouteDeclarations(List.of(routes), List.of()), false).plan().rules();
    }

    private static String render(DeclaredRoute... routes) {
        return new HttpRouteRenderer("{{ CustomBackendRef }}").generateHttpRoutesYaml(8081, plan(routes));
    }

    @Test
    void generatesYamlWithMatchesRewritesAndTimeouts() {
        String yaml = render(
                route("/api", "/api", HttpRoute.Type.INTERNAL, "a"),
                route("/gateway", "/svc", HttpRoute.Type.PUBLIC, 5_000, "b"),
                route("/items/{id}", "/items/{id}", HttpRoute.Type.PRIVATE, "c"));

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
        ).generateHttpRoutesYaml(8081, plan(route("/api", "/api", HttpRoute.Type.INTERNAL, "a")));

        assertTrue(yaml.contains("team:"));
        assertTrue(yaml.contains("platform"));
        assertTrue(yaml.contains("app.kubernetes.io/managed-by:"));
        assertTrue(yaml.contains("custom-manager"));
        assertFalse(yaml.contains("app.kubernetes.io/name:"));
        assertFalse(yaml.contains("deployment.netcracker.com/sessionId:"));
    }

    @Test
    void rendersExactSplitWithReplaceFullPath() {
        String yaml = render(
                route("/api/v1/svc/items", "/v1/items", HttpRoute.Type.INTERNAL, "list"),
                route("/api/v1/svc/items/{id}", "/v2/items/{id}", HttpRoute.Type.INTERNAL, "get"));

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
                        type: "Exact"
                        value: "/api/v1/svc/items/"
                    filters:
                    - type: "URLRewrite"
                      urlRewrite:
                        path:
                          type: "ReplaceFullPath"
                          replaceFullPath: "/v1/items/"
                    backendRefs:
                    - group: ""
                      kind: "Service"
                      name: "{{ CustomBackendRef }}"
                      port: 8081
                      weight: 1
                  - matches:
                    - path:
                        type: "Exact"
                        value: "/api/v1/svc/items"
                    filters:
                    - type: "URLRewrite"
                      urlRewrite:
                        path:
                          type: "ReplaceFullPath"
                          replaceFullPath: "/v1/items"
                    backendRefs:
                    - group: ""
                      kind: "Service"
                      name: "{{ CustomBackendRef }}"
                      port: 8081
                      weight: 1
                  - matches:
                    - path:
                        type: "PathPrefix"
                        value: "/api/v1/svc/items"
                    filters:
                    - type: "URLRewrite"
                      urlRewrite:
                        path:
                          type: "ReplacePrefixMatch"
                          replacePrefixMatch: "/v2/items"
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
                route("/facade/items", "/items", HttpRoute.Type.FACADE, "f"),
                route("/api/internal", "/api/internal", HttpRoute.Type.INTERNAL, "i"),
                route("/api/private", "/api/private", HttpRoute.Type.PRIVATE, "p"),
                route("/api/public", "/api/public", HttpRoute.Type.PUBLIC, "u"));

        int publicAt = yaml.indexOf("-java-annotations-public\"");
        int privateAt = yaml.indexOf("-java-annotations-private\"");
        int internalAt = yaml.indexOf("-java-annotations-internal\"");
        int facadeAt = yaml.indexOf("-java-annotations-facade\"");
        assertTrue(0 <= publicAt && publicAt < privateAt && privateAt < internalAt && internalAt < facadeAt, yaml);
    }

    @Test
    void serviceBoundRouteHasAllFacadeRulesAndOnlyTheServiceParentRef() {
        String yaml = render(
                route("/facade/items", "/items", HttpRoute.Type.FACADE, "f"),
                route("/orders", "/orders", HttpRoute.Type.FACADE, "o"));

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
                route("/items", "/items", HttpRoute.Type.FACADE, "f"),
                route("/api/public", "/api/public", HttpRoute.Type.PUBLIC, "u"));

        assertFalse(yaml.contains("-facade"), yaml);
        assertTrue(yaml.contains("-java-annotations-public"), yaml);
    }

    @Test
    void renderingTwiceGivesIdenticalOutput() {
        DeclaredRoute[] routes = {
                route("/facade/items", "/items", HttpRoute.Type.FACADE, "f"),
                route("/api/v1/svc/items", "/v1/items", HttpRoute.Type.PUBLIC, "list"),
                route("/api/v1/svc/items/{id}", "/v2/items/{id}", HttpRoute.Type.PUBLIC, "get"),
                route("/api/v1/svc/orders/{id}", "/orders/{id}", HttpRoute.Type.INTERNAL, 5_000, "o"),
                route("/api/v1/svc/admin", "/admin", HttpRoute.Type.PRIVATE, "a")};

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
    void sortsExactBeforePathPrefixWithTheSameValue() {
        List<PlannedRule> rules = new ArrayList<>(List.of(
                new PlannedRule(HttpRoute.Type.PUBLIC, PlannedRule.MatchType.PATH_PREFIX, "/a", null, null, 0, List.of()),
                new PlannedRule(HttpRoute.Type.PUBLIC, PlannedRule.MatchType.EXACT, "/a", null, null, 0, List.of()),
                new PlannedRule(HttpRoute.Type.PUBLIC, PlannedRule.MatchType.EXACT, "/a/", null, null, 0, List.of())));

        rules.sort(HttpRouteRenderer.ruleOrder());

        assertEquals(List.of("Exact /a/", "Exact /a", "PathPrefix /a"),
                rules.stream().map(r -> r.matchType().apiName() + " " + r.value()).toList());
    }
}
