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

    @Test
    void sixteenGatewayRulesFitInSingleHttpRoute() {
        HttpRouteRenderer renderer = new HttpRouteRenderer("svc", 8080, Map.of());
        String yaml = renderer.render(numberedRules("/gw", 16, false), List.of());
        String[] docs = splitDocs(yaml);
        assertEquals(1, docs.length);
        assertTrue(docs[0].contains("name: \"{{ .Values.SERVICE_NAME }}-java-annotations\""));
        assertFalse(yaml.contains("-java-annotations-2"));
        assertEquals(16, count(docs[0], "type: PathPrefix"));
    }

    @Test
    void seventeenGatewayRulesSplitIntoTwoHttpRoutesSortedLexically() {
        HttpRouteRenderer renderer = new HttpRouteRenderer("svc", 8080, Map.of());
        List<Rule> reversed = numberedRules("/gw", 17, false).reversed();
        String yaml = renderer.render(reversed, List.of());
        String[] docs = splitDocs(yaml);

        assertEquals(2, docs.length);
        assertTrue(docs[0].contains("name: \"{{ .Values.SERVICE_NAME }}-java-annotations\""));
        assertEquals(16, count(docs[0], "type: PathPrefix"));
        assertTrue(docs[0].contains("value: /gw-01"));
        assertTrue(docs[0].contains("value: /gw-16"));
        assertFalse(docs[0].contains("value: /gw-17"));
        assertTrue(docs[0].contains(HttpRouteRenderer.HTTPROUTES_GENERATOR_GATEWAYS_SNIPPET));
        assertTrue(docs[0].contains("app.kubernetes.io/name"));

        assertTrue(docs[1].contains("name: \"{{ .Values.SERVICE_NAME }}-java-annotations-2\""));
        assertEquals(1, count(docs[1], "type: PathPrefix"));
        assertTrue(docs[1].contains("value: /gw-17"));
        assertTrue(docs[1].contains(HttpRouteRenderer.HTTPROUTES_GENERATOR_GATEWAYS_SNIPPET));
        assertTrue(docs[1].contains("app.kubernetes.io/name"));
    }

    @Test
    void thirtyThreeGatewayRulesSplitIntoThreeHttpRoutes() {
        HttpRouteRenderer renderer = new HttpRouteRenderer("svc", 8080, Map.of());
        String yaml = renderer.render(numberedRules("/gw", 33, false), List.of());
        String[] docs = splitDocs(yaml);

        assertEquals(3, docs.length);
        assertTrue(docs[0].contains("name: \"{{ .Values.SERVICE_NAME }}-java-annotations\""));
        assertEquals(16, count(docs[0], "type: PathPrefix"));
        assertTrue(docs[1].contains("name: \"{{ .Values.SERVICE_NAME }}-java-annotations-2\""));
        assertEquals(16, count(docs[1], "type: PathPrefix"));
        assertTrue(docs[2].contains("name: \"{{ .Values.SERVICE_NAME }}-java-annotations-3\""));
        assertEquals(1, count(docs[2], "type: PathPrefix"));
        assertTrue(docs[2].contains("value: /gw-33"));
    }

    @Test
    void seventeenFacadeRulesSplitIntoTwoHttpRoutes() {
        HttpRouteRenderer renderer = new HttpRouteRenderer("svc", 8080, Map.of());
        String yaml = renderer.render(List.of(), numberedRules("/facade", 17, true));
        String[] docs = splitDocs(yaml);

        assertEquals(2, docs.length);
        assertFalse(yaml.contains("HTTPROUTES_GENERATOR_GATEWAYS"));

        assertTrue(docs[0].contains("name: \"{{ .Values.SERVICE_NAME }}-java-annotations-facade\""));
        assertEquals(16, count(docs[0], "type: PathPrefix"));
        assertTrue(docs[0].contains("group: \"\"\n    kind: Service\n    name: \"{{ .Values.SERVICE_NAME }}\""));

        assertTrue(docs[1].contains("name: \"{{ .Values.SERVICE_NAME }}-java-annotations-facade-2\""));
        assertEquals(1, count(docs[1], "type: PathPrefix"));
        assertTrue(docs[1].contains("value: /facade-17"));
        assertTrue(docs[1].contains("group: \"\"\n    kind: Service\n    name: \"{{ .Values.SERVICE_NAME }}\""));
    }

    @Test
    void mixedOverflowGatewayAndFacadeRoutesAreSeparatedAndDeterministic() {
        HttpRouteRenderer renderer = new HttpRouteRenderer("svc", 8080, Map.of());
        List<Rule> gateway = numberedRules("/gw", 17, false);
        List<Rule> facade = numberedRules("/facade", 17, true);
        String first = renderer.render(gateway, facade);
        String second = renderer.render(gateway, facade);
        assertEquals(first, second);

        String[] docs = splitDocs(first);
        assertEquals(4, docs.length);
        assertTrue(docs[0].contains("name: \"{{ .Values.SERVICE_NAME }}-java-annotations\""));
        assertTrue(docs[1].contains("name: \"{{ .Values.SERVICE_NAME }}-java-annotations-2\""));
        assertTrue(docs[2].contains("name: \"{{ .Values.SERVICE_NAME }}-java-annotations-facade\""));
        assertTrue(docs[3].contains("name: \"{{ .Values.SERVICE_NAME }}-java-annotations-facade-2\""));
    }

    private List<Rule> numberedRules(String prefix, int count, boolean rewrite) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> {
                    String match = String.format("%s-%02d", prefix, i);
                    String target = rewrite ? String.format("/svc-%02d", i) : match;
                    return rule(match, target, 0);
                })
                .toList();
    }

    private String[] splitDocs(String yaml) {
        return yaml.split("(?m)^---\\R");
    }

    private int count(String text, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    private Rule rule(String match, String target, long timeout) {
        return new Rule(match, target, timeout, Set.of("com.example.Controller"));
    }
}
