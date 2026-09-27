package com.netcracker.routes.gateway.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.netcracker.routes.gateway.plugin.Gateway.INTERNAL;
import static com.netcracker.routes.gateway.plugin.Gateway.PRIVATE;
import static com.netcracker.routes.gateway.plugin.Gateway.PUBLIC;
import static com.netcracker.routes.gateway.plugin.LegacyRouteTableTest.forbidden;
import static com.netcracker.routes.gateway.plugin.LegacyRouteTableTest.route;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationPolicyRendererTest {

    private static final String RESOURCE = "/api/v1/svc/resource";
    private static final String ORDER = "/api/v1/svc/order";

    private static String render(Map<Gateway, List<String>> ports, List<DeclaredRoute> routes, List<ForbiddenDeclaration> forbidden) {
        RouteMigration migration = RouteMigration.run(new RouteDeclarations(routes, forbidden), false);
        assertFalse(migration.hasErrors(), migration.findings().toString());
        return new AuthorizationPolicyRenderer(Map.of(), ports).generateAuthorizationPoliciesYaml(migration.plan().denyRules());
    }

    @Test
    void forbiddenOnPublicAndPrivate() {
        String yaml = render(Map.of(), List.of(
                route(RESOURCE, RESOURCE, HttpRoute.Type.PUBLIC, "com.acme.ResourceController"),
                route(RESOURCE + "/{id}/internal-api", RESOURCE + "/{id}/internal-api", HttpRoute.Type.INTERNAL,
                        "com.acme.ResourceController#internalApi"),
                route(RESOURCE + "/{id}/internal-api/status", RESOURCE + "/{id}/internal-api/status", HttpRoute.Type.PUBLIC,
                        "com.acme.ResourceController#status")),
                List.of(forbidden(RESOURCE + "/{id}/internal-api", "com.acme.ResourceController#internalApi", PUBLIC, PRIVATE)));

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
    void noForbiddenDeclarations() {
        String yaml = render(Map.of(), List.of(route(RESOURCE, RESOURCE, HttpRoute.Type.PUBLIC, "c")), List.of());

        assertEquals("", yaml);
    }

    @Test
    void portsForOneGateway() {
        String yaml = render(Map.of(INTERNAL, List.of("8080", "8443")),
                List.of(route(ORDER + "/{id}/items", ORDER + "/{id}/items", HttpRoute.Type.PUBLIC, "items")),
                List.of(forbidden(ORDER, "com.acme.OrderController", PUBLIC, PRIVATE, INTERNAL)));

        String[] policies = yaml.split("---\n");
        assertEquals(4, policies.length, yaml);
        assertTrue(policies[1].contains("deny-public\"") && policies[1].contains("ports:\n        - \"8080\"\n        paths:"), policies[1]);
        assertTrue(policies[2].contains("deny-private\"") && policies[2].contains("ports:\n        - \"8080\"\n        paths:"), policies[2]);
        assertTrue(policies[3].contains("deny-internal\"")
                && policies[3].contains("ports:\n        - \"8080\"\n        - \"8443\"\n        paths:"), policies[3]);
        assertTrue(policies[3].contains("""
                  targetRefs:
                  - group: ""
                    kind: "Service"
                    name: "internal-gateway-service"
                """), policies[3]);
    }

    @Test
    void customLabels() {
        List<DenyRule> rules = List.of(new DenyRule(PUBLIC, List.of("/a", "/a/{**}"), List.of(), "x"));

        String yaml = new AuthorizationPolicyRenderer(Map.of("team", "platform"), Map.of()).generateAuthorizationPoliciesYaml(rules);

        assertTrue(yaml.contains("  labels:\n    team: \"platform\"\nspec:"), yaml);
        assertFalse(yaml.contains("notPaths"), yaml);
    }
}
