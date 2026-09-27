package com.netcracker.routes.gateway.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders the planned DENY rules (design D8, D9) as one {@code AuthorizationPolicy} per border gateway,
 * in the order public, private, internal.
 */
public class AuthorizationPolicyRenderer {

    public static final List<String> DEFAULT_PORTS = List.of("8080");
    private static final String ACTION_DENY = "DENY";

    private final Map<String, String> labels;
    private final Map<Gateway, List<String>> ports;

    /**
     * @param labels custom labels, or empty for the default labels
     * @param ports  ports per gateway; a gateway that isn't listed uses {@link #DEFAULT_PORTS}
     */
    public AuthorizationPolicyRenderer(Map<String, String> labels, Map<Gateway, List<String>> ports) {
        this.labels = labels == null ? Collections.emptyMap() : labels;
        this.ports = ports == null ? Collections.emptyMap() : ports;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AuthorizationPolicyResource(String apiVersion, String kind, Metadata metadata, Spec spec) {

        public AuthorizationPolicyResource(Metadata metadata, Spec spec) {
            this("security.istio.io/v1", "AuthorizationPolicy", metadata, spec);
        }

        public record Metadata(String name, Map<String, String> labels) {
        }

        public record Spec(List<TargetRef> targetRefs, String action, List<Rule> rules) {

            public record TargetRef(String group, String kind, String name) {
            }

            public record Rule(List<To> to) {

                public record To(Operation operation) {
                }

                @JsonInclude(JsonInclude.Include.NON_EMPTY)
                public record Operation(List<String> ports, List<String> paths, List<String> notPaths) {
                }
            }
        }
    }

    public String generateAuthorizationPoliciesYaml(List<DenyRule> denyRules) {
        List<AuthorizationPolicyResource> policies = new ArrayList<>();
        for (Gateway gateway : Gateway.values()) {
            List<DenyRule> rules = denyRules.stream().filter(r -> r.gateway() == gateway).toList();
            if (!rules.isEmpty()) {
                policies.add(toResource(gateway, rules));
            }
        }
        return policies.stream()
                .map(ResourceLabels::writeYaml)
                .collect(Collectors.joining());
    }

    private AuthorizationPolicyResource toResource(Gateway gateway, List<DenyRule> rules) {
        AuthorizationPolicyResource.Metadata metadata = new AuthorizationPolicyResource.Metadata(
                "{{ .Values.SERVICE_NAME }}-java-annotations-deny-" + gateway.shortName(),
                ResourceLabels.of(labels));
        AuthorizationPolicyResource.Spec.TargetRef targetRef =
                new AuthorizationPolicyResource.Spec.TargetRef(gateway.refGroup(), gateway.refKind(), gateway.refName());
        List<String> gatewayPorts = ports.getOrDefault(gateway, DEFAULT_PORTS);
        List<AuthorizationPolicyResource.Spec.Rule> ruleList = rules.stream()
                .map(rule -> new AuthorizationPolicyResource.Spec.Rule(List.of(new AuthorizationPolicyResource.Spec.Rule.To(
                        new AuthorizationPolicyResource.Spec.Rule.Operation(gatewayPorts, rule.paths(), rule.notPaths())))))
                .toList();
        return new AuthorizationPolicyResource(metadata,
                new AuthorizationPolicyResource.Spec(List.of(targetRef), ACTION_DENY, ruleList));
    }
}
