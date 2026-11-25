package com.netcracker.routes.gateway.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RootReductionTest {

    @Test
    void endpointsAbsorbedByControllerRoot() {
        List<Rule> rules = List.of(
                rule("/api/v1/test1", "/api/v1/test1", 0),
                rule("/api/v1/test1/test1", "/api/v1/test1/test1", 250_000),
                rule("/api/v1/test1/test1/test2", "/api/v1/test1/test1/test2", 150_000)
        );

        List<Rule> roots = RootReducer.reduce(rules);

        assertEquals(1, roots.size());
        Rule root = roots.get(0);
        assertEquals("/api/v1/test1", root.match());
        assertEquals("/api/v1/test1", root.target());
        assertEquals(250_000, root.timeout());
    }

    @Test
    void consistentRewriteAbsorbed() {
        List<Rule> rules = List.of(
                rule("/class/path/from", "/class/path/to", 0),
                rule("/class/path/from/method/path/to2", "/class/path/to/method/path/to2", 0)
        );

        List<Rule> roots = RootReducer.reduce(rules);

        assertEquals(1, roots.size());
        assertEquals("/class/path/from", roots.get(0).match());
        assertEquals("/class/path/to", roots.get(0).target());
    }

    @Test
    void differentNestedRewriteKept() {
        List<Rule> rules = List.of(
                rule("/api", "/api", 0),
                rule("/api/legacy", "/v2/new", 0)
        );

        List<Rule> roots = RootReducer.reduce(rules);

        assertEquals(2, roots.size());
        assertEquals("/api", roots.get(0).match());
        assertEquals("/api", roots.get(0).target());
        assertEquals("/api/legacy", roots.get(1).match());
        assertEquals("/v2/new", roots.get(1).target());
    }

    @Test
    void endpointsWithoutControllerRoot() {
        List<Rule> rules = List.of(
                rule("/test1", "/test1", 0),
                rule("/test1/test2", "/test1/test2", 0),
                rule("/test2", "/test2", 0)
        );

        List<Rule> roots = RootReducer.reduce(rules);

        assertEquals(2, roots.size());
        assertEquals("/test1", roots.get(0).match());
        assertEquals("/test2", roots.get(1).match());
    }

    @Test
    void conflictingTargetsFail() {
        List<Rule> rules = List.of(
                rule("/gw", "/svc-a", 0),
                rule("/gw", "/svc-b", 0)
        );

        RouteGenerationException ex = assertThrows(RouteGenerationException.class, () -> RootReducer.reduce(rules));
        assertTrue(ex.getMessage().contains("/gw"));
    }

    @Test
    void rootSlashFails() {
        List<Rule> rules = List.of(rule("/", "/", 0));

        RouteGenerationException ex = assertThrows(RouteGenerationException.class, () -> RootReducer.reduce(rules));
        assertTrue(ex.getMessage().contains("com.example.MyController"));
    }

    @Test
    void groupsReducedIndependently() {
        List<Rule> gatewayRules = List.of(rule("/gw", "/gw", 0));
        List<Rule> facadeRules = List.of(rule("/gw", "/svc", 0));

        List<Rule> gatewayRoots = RootReducer.reduce(gatewayRules);
        List<Rule> facadeRoots = RootReducer.reduce(facadeRules);

        assertEquals("/gw", gatewayRoots.get(0).target());
        assertEquals("/svc", facadeRoots.get(0).target());
    }

    @Test
    void facadeRootWithRewriteRendersGroup() {
        List<Rule> facadeRoots = List.of(rule("/test/sleep", "/sleep", 0));

        List<Rule> rendered = RootReducer.applyFacadeFilter(facadeRoots);

        assertEquals(1, rendered.size());
    }

    @Test
    void facadeRootWithLongTimeoutRendersGroup() {
        List<Rule> facadeRoots = List.of(rule("/api/v1/reports", "/api/v1/reports", 300_000));

        List<Rule> rendered = RootReducer.applyFacadeFilter(facadeRoots);

        assertEquals(1, rendered.size());
    }

    @Test
    void oneFacadeRootThatNeedsItRendersTheWholeGroup() {
        List<Rule> facadeRoots = List.of(
                rule("/test/sleep", "/sleep", 0),
                rule("/api/v1/plain", "/api/v1/plain", 10_000)
        );

        List<Rule> rendered = RootReducer.applyFacadeFilter(facadeRoots);

        assertEquals(2, rendered.size());
    }

    @Test
    void facadeGroupWithNothingToChangeRendersNothing() {
        List<Rule> facadeRoots = List.of(rule("/class/path/to/method/path/to", "/class/path/to/method/path/to", 10_000));

        List<Rule> rendered = RootReducer.applyFacadeFilter(facadeRoots);

        assertTrue(rendered.isEmpty());
    }

    @Test
    void timeoutExactlyTwoMinutesRendersNothing() {
        List<Rule> facadeRoots = List.of(rule("/api/v1/reports", "/api/v1/reports", 120_000));

        List<Rule> rendered = RootReducer.applyFacadeFilter(facadeRoots);

        assertTrue(rendered.isEmpty());
    }

    private Rule rule(String match, String target, long timeout) {
        return new Rule(match, target, timeout, Set.of("com.example.MyController"));
    }
}
