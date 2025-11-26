package com.netcracker.cloud.routesregistration.common.gateway.route.v3.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteV3Test {

    private RouteDestination destination;
    private RouteV3 route1;
    private RouteV3 route2;
    private Rule ruleA;
    private Rule ruleB;
    private Rule ruleDuplicate;

    @BeforeEach
    void setUp() {
        destination = RouteDestination.builder()
                .cluster("cluster-1")
                .endpoint("endpoint-1")
                .build();

        ruleA = Rule.builder()
                .prefixRewrite("/api/v1")
                .allowed(true)
                .timeout(1000L)
                .build();

        ruleB = Rule.builder()
                .prefixRewrite("/api/v2")
                .allowed(false)
                .timeout(2000L)
                .build();

        ruleDuplicate = Rule.builder()
                .prefixRewrite("/api/v1")
                .allowed(true)
                .timeout(1000L)
                .build(); // identical to ruleA

        route1 = RouteV3.builder()
                .destination(destination)
                .rules(List.of(ruleA))
                .build();

        route2 = RouteV3.builder()
                .destination(destination)
                .rules(List.of(ruleB, ruleDuplicate))
                .build();
    }

    @Test
    void merge_shouldCombineUniqueRules() {
        RouteV3 merged = route1.merge(route2);

        assertEquals(2, route1.getRules().size());
        assertTrue(route1.getRules().contains(ruleA));
        assertTrue(route1.getRules().contains(ruleB));
        assertTrue(route1.getRules().contains(ruleDuplicate));
        assertSame(merged, route1);
    }

    @Test
    void merge_shouldNotAddDuplicateRules() {
        route1.merge(RouteV3.builder()
                .destination(destination)
                .rules(List.of(ruleDuplicate))
                .build());

        assertEquals(1, route1.getRules().size(), "Duplicate rule should not be added");
    }

    @Test
    void merge_shouldThrowExceptionForDifferentDestinations() {
        RouteDestination otherDestination = RouteDestination.builder()
                .cluster("cluster-2")
                .endpoint("endpoint-2")
                .build();

        RouteV3 routeWithDifferentDest = RouteV3.builder()
                .destination(otherDestination)
                .rules(List.of(ruleB))
                .build();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> route1.merge(routeWithDifferentDest)
        );

        assertEquals("Cannot merge RouteV3 with different RouteDestination", ex.getMessage());
    }

    @Test
    void merge_shouldHandleEmptyRules() {
        RouteV3 emptyRulesRoute = RouteV3.builder()
                .destination(destination)
                .rules(List.of())
                .build();

        route1.merge(emptyRulesRoute);
        assertEquals(1, route1.getRules().size());
    }

    @Test
    void merge_shouldHandleNullRuleListsSafely() {
        RouteV3 nullRulesRoute = RouteV3.builder()
                .destination(destination)
                .rules(null)
                .build();

        assertDoesNotThrow(() -> route1.merge(nullRulesRoute));
        assertEquals(1, route1.getRules().size());
    }
}
