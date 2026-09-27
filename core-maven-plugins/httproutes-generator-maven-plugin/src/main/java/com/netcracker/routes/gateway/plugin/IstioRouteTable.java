package com.netcracker.routes.gateway.plugin;

import java.util.List;

/**
 * How Istio routes requests on a border gateway with the planned HTTPRoute rules and DENY rules attached to it.
 * A request is checked against the DENY rules first, then against {@code Exact} rules,
 * then against the longest segment-aligned {@code PathPrefix}.
 */
public final class IstioRouteTable {

    private final List<PlannedRule> rules;
    private final List<IstioPlan.ConflictedPrefix> conflicted;
    private final List<DenyRule> denyRules;

    private IstioRouteTable(List<PlannedRule> rules, List<IstioPlan.ConflictedPrefix> conflicted, List<DenyRule> denyRules) {
        this.rules = List.copyOf(rules);
        this.conflicted = List.copyOf(conflicted);
        this.denyRules = List.copyOf(denyRules);
    }

    public static IstioRouteTable of(IstioPlan plan) {
        return new IstioRouteTable(plan.rules(), plan.conflicted(), plan.denyRules());
    }

    /**
     * @return the table of the HTTPRoute rules without any DENY rules, used to decide where automatic DENY rules are needed
     */
    public static IstioRouteTable routingOnly(List<PlannedRule> rules, List<IstioPlan.ConflictedPrefix> conflicted) {
        return new IstioRouteTable(rules, conflicted, List.of());
    }

    public Decision decide(Gateway gateway, String requestPath) {
        for (DenyRule rule : denyRules) {
            if (rule.gateway() == gateway && rule.denies(requestPath)) {
                return new Decision.Denied(rule);
            }
        }
        List<PlannedRule> attached = rules.stream().filter(r -> r.gateways().contains(gateway)).toList();
        for (PlannedRule rule : attached) {
            if (rule.matchType() == PlannedRule.MatchType.EXACT && rule.matches(requestPath)) {
                return routed(rule, requestPath);
            }
        }
        PlannedRule longest = null;
        for (PlannedRule rule : attached) {
            if (rule.matchType() == PlannedRule.MatchType.PATH_PREFIX && rule.matches(requestPath)
                    && (longest == null || rule.prefixBase().length() > longest.prefixBase().length())) {
                longest = rule;
            }
        }
        for (IstioPlan.ConflictedPrefix prefix : conflicted) {
            if (prefix.gateways().contains(gateway) && prefix.matches(requestPath)
                    && (longest == null || prefix.prefixBase().length() > longest.prefixBase().length())) {
                return new Decision.Conflicted(prefix);
            }
        }
        return longest == null ? new Decision.Unrouted() : routed(longest, requestPath);
    }

    private static Decision routed(PlannedRule rule, String requestPath) {
        return new Decision.Routed(rule.upstream(requestPath), rule.timeout(), rule);
    }

    /**
     * Istio decision for one request path on one gateway.
     */
    public sealed interface Decision {

        record Routed(String upstreamPath, long timeout, PlannedRule rule) implements Decision {
        }

        record Denied(DenyRule rule) implements Decision {
        }

        /**
         * The longest matching {@code PathPrefix} is a conflicted one, which got no rule. The conflict is reported already.
         */
        record Conflicted(IstioPlan.ConflictedPrefix prefix) implements Decision {
        }

        record Unrouted() implements Decision {
        }
    }
}
