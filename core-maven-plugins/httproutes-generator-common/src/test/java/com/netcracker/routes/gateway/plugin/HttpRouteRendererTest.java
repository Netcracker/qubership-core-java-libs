package com.netcracker.routes.gateway.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRouteRendererTest {

    @Test
    void defaultBackendRef() {
        HttpRouteRenderer renderer = new HttpRouteRenderer("{{ .Values.SERVICE_NAME }}", 8080, Map.of());
        String yaml = renderer.render(List.of(rule("/api", "/api", 0)), List.of());
        assertTrue(yaml.contains("name: \"{{ .Values.SERVICE_NAME }}\""));
    }

    @Test
    void customBackendRef() {
        HttpRouteRenderer renderer = new HttpRouteRenderer("{{ .Values.DEPLOYMENT_RESOURCE_NAME }}", 8080, Map.of());
        String yaml = renderer.render(List.of(rule("/api", "/api", 0)), List.of());
        assertTrue(yaml.contains("name: \"{{ .Values.DEPLOYMENT_RESOURCE_NAME }}\""));
    }

    @Test
    void gatewaysSnippetTextIsExact() {
        HttpRouteRenderer renderer = new HttpRouteRenderer("svc", 8080, Map.of());
        String yaml = renderer.render(List.of(rule("/api", "/api", 0)), List.of());
        assertTrue(yaml.contains(HttpRouteRenderer.HTTPROUTES_GENERATOR_GATEWAYS_SNIPPET));
        assertTrue(yaml.contains("default (list \"public-gateway\" \"private-gateway\" \"internal-gateway-service\")"));
        assertTrue(yaml.contains("kind: {{ if eq $name \"internal-gateway-service\" }}Service{{ else }}Gateway{{ end }}"));
        assertTrue(yaml.contains("group: {{ if eq $name \"internal-gateway-service\" }}\"\"{{ else }}gateway.networking.k8s.io{{ end }}"));
    }

    @Test
    void facadeResourceHasNoGatewaysSnippetAndOwnParentRef() {
        HttpRouteRenderer renderer = new HttpRouteRenderer("svc", 8080, Map.of());
        String yaml = renderer.render(List.of(), List.of(rule("/facade", "/svc", 0)));
        assertFalse(yaml.contains("HTTPROUTES_GENERATOR_GATEWAYS"));
        assertTrue(yaml.contains("name: \"{{ .Values.SERVICE_NAME }}-java-annotations-facade\""));
        assertTrue(yaml.contains("group: \"\"\n    kind: Service\n    name: \"{{ .Values.SERVICE_NAME }}\""));
    }

    @Test
    void gatewayAndFacadeRulesStayInTheirOwnResource() {
        HttpRouteRenderer renderer = new HttpRouteRenderer("svc", 8080, Map.of());
        String yaml = renderer.render(List.of(rule("/gw", "/gw", 0)), List.of(rule("/facade", "/svc2", 0)));
        int gatewayDocEnd = yaml.indexOf("---");
        String gatewayDoc = yaml.substring(0, gatewayDocEnd);
        String facadeDoc = yaml.substring(gatewayDocEnd);
        assertTrue(gatewayDoc.contains("/gw"));
        assertFalse(gatewayDoc.contains("/facade"));
        assertTrue(facadeDoc.contains("/facade"));
        assertFalse(facadeDoc.contains("/gw\n"));
    }

    @Test
    void urlRewriteOnlyWhenMatchDiffersFromTarget() {
        HttpRouteRenderer renderer = new HttpRouteRenderer("svc", 8080, Map.of());
        String noRewrite = renderer.render(List.of(rule("/api", "/api", 0)), List.of());
        assertFalse(noRewrite.contains("URLRewrite"));

        String withRewrite = renderer.render(List.of(rule("/gw", "/svc", 0)), List.of());
        assertTrue(withRewrite.contains("URLRewrite"));
        assertTrue(withRewrite.contains("replacePrefixMatch: /svc"));
    }

    @Test
    void timeoutFormattedInSeconds() {
        HttpRouteRenderer renderer = new HttpRouteRenderer("svc", 8080, Map.of());
        String yaml = renderer.render(List.of(rule("/api", "/api", 5000)), List.of());
        assertTrue(yaml.contains("request: 5s"));
    }

    @Test
    void noRegexOrManualReviewBanner() {
        HttpRouteRenderer renderer = new HttpRouteRenderer("svc", 8080, Map.of());
        String yaml = renderer.render(List.of(rule("/api/{id}", "/api/{id}", 0)), List.of(rule("/facade/{id}", "/other/{id}", 0)));
        assertFalse(yaml.contains("RegularExpression"));
        assertFalse(yaml.contains("MANUAL REVIEW"));
    }

    @Test
    void labelReplacement() {
        HttpRouteRenderer renderer = new HttpRouteRenderer("svc", 8080, Map.of("team", "platform"));
        String yaml = renderer.render(List.of(rule("/api", "/api", 0)), List.of());
        assertTrue(yaml.contains("team: platform"));
        assertFalse(yaml.contains("app.kubernetes.io/name"));
    }

    @Test
    void deterministicOutput() {
        HttpRouteRenderer renderer = new HttpRouteRenderer("svc", 8080, Map.of());
        List<Rule> gateway = List.of(rule("/api", "/api", 0), rule("/other", "/svc-other", 1000));
        List<Rule> facade = List.of(rule("/facade", "/svc-facade", 0));
        String first = renderer.render(gateway, facade);
        String second = renderer.render(gateway, facade);
        assertEquals(first, second);
    }

    private Rule rule(String match, String target, long timeout) {
        return new Rule(match, target, timeout, Set.of("com.example.Controller"));
    }
}
