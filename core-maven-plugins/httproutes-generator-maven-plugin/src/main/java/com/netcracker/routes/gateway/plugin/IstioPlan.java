package com.netcracker.routes.gateway.plugin;

import java.util.List;
import java.util.Set;

/**
 * Everything the planner decided to generate.
 *
 * @param rules      HTTPRoute rules of all resources, the service-bound one included
 * @param conflicted {@code PathPrefix} values that got no rule because their routes conflict
 * @param denyRules  AuthorizationPolicy DENY rules, sorted by gateway
 * @param findings   generation-time findings
 * @param sample     path segment that collides with no declared literal segment, used for example requests
 */
public record IstioPlan(List<PlannedRule> rules, List<ConflictedPrefix> conflicted, List<DenyRule> denyRules,
                        List<Finding> findings, String sample) {

    public IstioPlan {
        rules = List.copyOf(rules);
        conflicted = List.copyOf(conflicted);
        denyRules = List.copyOf(denyRules);
        findings = List.copyOf(findings);
    }

    public List<PlannedRule> rules(HttpRoute.Type resourceType) {
        return rules.stream().filter(r -> r.resourceType() == resourceType).toList();
    }

    public List<DenyRule> denyRules(Gateway gateway) {
        return denyRules.stream().filter(r -> r.gateway() == gateway).toList();
    }

    /**
     * A {@code PathPrefix} value whose routes could not share one rule. It gets no rule, and the conflict is reported.
     *
     * @param resourceType the resource the rule would have gone into
     */
    public record ConflictedPrefix(String value, HttpRoute.Type resourceType) {

        public Set<Gateway> gateways() {
            return Gateway.exposedBy(resourceType);
        }

        public boolean matches(String requestPath) {
            return PlannedRule.prefixMatches(value, requestPath);
        }

        String prefixBase() {
            return PlannedRule.prefixBase(value);
        }
    }
}
