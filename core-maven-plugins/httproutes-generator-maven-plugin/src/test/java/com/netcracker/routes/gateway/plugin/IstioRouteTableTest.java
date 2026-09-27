package com.netcracker.routes.gateway.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.netcracker.routes.gateway.plugin.Gateway.INTERNAL;
import static com.netcracker.routes.gateway.plugin.Gateway.PUBLIC;
import static com.netcracker.routes.gateway.plugin.LegacyRouteTableTest.route;
import static com.netcracker.routes.gateway.plugin.PlannedRule.MatchType.EXACT;
import static com.netcracker.routes.gateway.plugin.PlannedRule.MatchType.PATH_PREFIX;
import static com.netcracker.routes.gateway.plugin.PlannedRule.RewriteType.REPLACE_FULL_PATH;
import static com.netcracker.routes.gateway.plugin.PlannedRule.RewriteType.REPLACE_PREFIX_MATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class IstioRouteTableTest {

    private static final DeclaredRoute SOURCE = route("/a", "/a", HttpRoute.Type.PUBLIC, "a");

    private static PlannedRule rule(HttpRoute.Type type, PlannedRule.MatchType match, String value,
                                    PlannedRule.RewriteType rewrite, String rewriteValue) {
        return new PlannedRule(type, match, value, rewrite, rewriteValue, 0, List.of(SOURCE));
    }

    private static IstioPlan plan(List<PlannedRule> rules, List<DenyRule> denyRules) {
        return new IstioPlan(rules, List.of(), denyRules, List.of(), "x-probe-0");
    }

    private static IstioRouteTable.Decision.Routed assertRouted(IstioRouteTable table, Gateway gateway, String request,
                                                                String upstream) {
        IstioRouteTable.Decision decision = table.decide(gateway, request);
        IstioRouteTable.Decision.Routed routed = assertInstanceOf(IstioRouteTable.Decision.Routed.class, decision,
                gateway + " " + request + ": " + decision);
        assertEquals(upstream, routed.upstreamPath(), gateway + " " + request);
        return routed;
    }

    @Test
    void exactWinsOverLongerPrefix() {
        PlannedRule exact = rule(HttpRoute.Type.PUBLIC, EXACT, "/a", REPLACE_FULL_PATH, "/v1/a");
        PlannedRule prefix = rule(HttpRoute.Type.PUBLIC, PATH_PREFIX, "/a", REPLACE_PREFIX_MATCH, "/v2/a");
        IstioRouteTable table = IstioRouteTable.of(plan(List.of(prefix, exact), List.of()));

        assertSame(exact, assertRouted(table, PUBLIC, "/a", "/v1/a").rule());
        assertSame(prefix, assertRouted(table, PUBLIC, "/a/1", "/v2/a/1").rule());
    }

    @Test
    void longestSegmentAlignedPrefixWins() {
        PlannedRule shorter = rule(HttpRoute.Type.PUBLIC, PATH_PREFIX, "/a", null, null);
        PlannedRule longer = rule(HttpRoute.Type.PUBLIC, PATH_PREFIX, "/a/b", REPLACE_PREFIX_MATCH, "/x");
        IstioRouteTable table = IstioRouteTable.of(plan(List.of(shorter, longer), List.of()));

        assertRouted(table, PUBLIC, "/a/b/c", "/x/c");
        assertRouted(table, PUBLIC, "/a/bc", "/a/bc");
        assertInstanceOf(IstioRouteTable.Decision.Unrouted.class, table.decide(PUBLIC, "/ab"));
    }

    @Test
    void replacePrefixMatchWithRootDoesNotDoubleSlash() {
        IstioRouteTable table = IstioRouteTable.of(plan(
                List.of(rule(HttpRoute.Type.PUBLIC, PATH_PREFIX, "/api/a", REPLACE_PREFIX_MATCH, "/")), List.of()));

        assertRouted(table, PUBLIC, "/api/a/x", "/x");
        assertRouted(table, PUBLIC, "/api/a", "/");
        assertRouted(table, PUBLIC, "/api/a/", "/");
    }

    @Test
    void rulesAttachOnlyToGatewaysOfTheirResource() {
        IstioRouteTable table = IstioRouteTable.of(plan(
                List.of(rule(HttpRoute.Type.INTERNAL, PATH_PREFIX, "/a", null, null),
                        rule(HttpRoute.Type.FACADE, PATH_PREFIX, "/b", null, null)), List.of()));

        assertRouted(table, INTERNAL, "/a", "/a");
        assertInstanceOf(IstioRouteTable.Decision.Unrouted.class, table.decide(PUBLIC, "/a"));
        assertInstanceOf(IstioRouteTable.Decision.Unrouted.class, table.decide(INTERNAL, "/b"));
    }

    @Test
    void denyRulesComeFirstAndHonorNotPaths() {
        DenyRule deny = new DenyRule(PUBLIC, DenyRule.pathAndSubtree("/a/{*}/internal"),
                DenyRule.pathAndSubtree("/a/{*}/internal/status"), "internal");
        IstioPlan plan = plan(List.of(rule(HttpRoute.Type.PUBLIC, PATH_PREFIX, "/a", null, null)), List.of(deny));
        IstioRouteTable table = IstioRouteTable.of(plan);

        assertInstanceOf(IstioRouteTable.Decision.Denied.class, table.decide(PUBLIC, "/a/1/internal"));
        assertInstanceOf(IstioRouteTable.Decision.Denied.class, table.decide(PUBLIC, "/a/1/internal/x/y"));
        assertRouted(table, PUBLIC, "/a/1/internal/status", "/a/1/internal/status");
        assertRouted(table, PUBLIC, "/a/1/2/internal", "/a/1/2/internal");
        assertRouted(table, Gateway.PRIVATE, "/a/1/internal", "/a/1/internal");
        assertRouted(IstioRouteTable.routingOnly(plan.rules(), plan.conflicted()), PUBLIC, "/a/1/internal", "/a/1/internal");
    }

    @Test
    void longerConflictedPrefixHidesShorterRule() {
        IstioPlan plan = new IstioPlan(List.of(rule(HttpRoute.Type.PUBLIC, PATH_PREFIX, "/a", null, null)),
                List.of(new IstioPlan.ConflictedPrefix("/a/b", HttpRoute.Type.PUBLIC)), List.of(), List.of(), "x-probe-0");
        IstioRouteTable table = IstioRouteTable.of(plan);

        assertInstanceOf(IstioRouteTable.Decision.Conflicted.class, table.decide(PUBLIC, "/a/b/c"));
        assertRouted(table, PUBLIC, "/a/c", "/a/c");
    }
}
