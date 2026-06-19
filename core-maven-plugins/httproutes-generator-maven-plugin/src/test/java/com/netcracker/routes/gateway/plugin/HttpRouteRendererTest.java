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

    @Test
    void generatesYamlWithMatchesRewritesAndTimeouts() {
        Set<HttpRoute> routes = Set.of(
                new HttpRoute("/api", HttpRoute.Type.INTERNAL),
                new HttpRoute("/svc", "/gateway", HttpRoute.Type.PUBLIC, 5_000),
                new HttpRoute("/items/{id}", HttpRoute.Type.PRIVATE)
        );

        String yaml = new HttpRouteRenderer("{{ CustomBackendRef }}").generateHttpRoutesYaml(8081, routes);

        assertTrue(yaml.contains("HTTPRoute"));
        assertTrue(yaml.contains("ReplacePrefixMatch"));
        assertTrue(yaml.contains("request: \"5s\""));
        assertTrue(yaml.contains("RegularExpression"));
        assertTrue(yaml.contains("items/([^/]+)"));
        assertTrue(yaml.contains("{{ CustomBackendRef }}"));
        assertTrue(yaml.contains("# MANUAL REVIEW REQUIRED"));
    }

    @Test
    void replacesDefaultLabelsWhenCustomLabelsProvided() {
        Set<HttpRoute> routes = Set.of(new HttpRoute("/api", HttpRoute.Type.INTERNAL));

        String yaml = new HttpRouteRenderer(
                "{{ CustomBackendRef }}",
                Map.of(
                        "team", "platform",
                        "app.kubernetes.io/managed-by", "custom-manager"
                )
        ).generateHttpRoutesYaml(8081, routes);

        assertTrue(yaml.contains("team:"));
        assertTrue(yaml.contains("platform"));
        assertTrue(yaml.contains("app.kubernetes.io/managed-by:"));
        assertTrue(yaml.contains("custom-manager"));
        assertFalse(yaml.contains("app.kubernetes.io/name:"));
        assertFalse(yaml.contains("deployment.netcracker.com/sessionId:"));
    }

    @Test
    void sortsRulesByPathSpecificity() {
        List<HttpRoute> routes = new ArrayList<>(List.of(
                new HttpRoute("/alpha", HttpRoute.Type.INTERNAL),
                new HttpRoute("/alpha/bravo", HttpRoute.Type.INTERNAL),
                new HttpRoute("/alpha/bravo/charlie", HttpRoute.Type.INTERNAL)
        ));

        routes.sort(HttpRouteRenderer.pathSpecificityComparator());

        assertEquals("/alpha/bravo/charlie", routes.get(0).gatewayPath());
        assertEquals("/alpha/bravo", routes.get(1).gatewayPath());
        assertEquals("/alpha", routes.get(2).gatewayPath());
    }

    @Test
    void sortsByLengthThenLexicalWhenSegmentsEqual() {
        List<HttpRoute> routes = new ArrayList<>(List.of(
                new HttpRoute("/aa/za", HttpRoute.Type.INTERNAL),
                new HttpRoute("/aa/ab", HttpRoute.Type.INTERNAL),
                new HttpRoute("/aa/abcd", HttpRoute.Type.INTERNAL)
        ));

        routes.sort(HttpRouteRenderer.pathSpecificityComparator());

        assertEquals("/aa/abcd", routes.get(0).gatewayPath());
        assertEquals("/aa/ab", routes.get(1).gatewayPath());
        assertEquals("/aa/za", routes.get(2).gatewayPath());
    }
}
