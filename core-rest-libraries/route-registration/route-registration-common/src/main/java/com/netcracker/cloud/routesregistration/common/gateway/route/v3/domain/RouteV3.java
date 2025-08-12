package com.netcracker.cloud.routesregistration.common.gateway.route.v3.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Objects;

@Data
@Builder
public class RouteV3 {
    private RouteDestination destination;
    private List<Rule> rules;

    public RouteV3 merge(RouteV3 anotherRoute) {
        if (!destination.equals(anotherRoute.getDestination())) {
            throw new IllegalArgumentException("Cannot merge RouteV3 with different RouteDestination");
        }
        mergeRules(anotherRoute.rules);
        return this;
    }

    private void mergeRules(List<Rule> rulesToMerge) {
        rulesToMerge.forEach(ruleToMerge -> {
            if (rules.stream().noneMatch(ruleToMerge::equals)) {
                rules.add(ruleToMerge);
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteV3 routeV3 = (RouteV3) o;
        if (!Objects.equals(destination, routeV3.destination)) {
            return false;
        }
        return rules != null && routeV3.rules != null && rules.size() == routeV3.rules.size()
                && rules.containsAll(routeV3.rules) && routeV3.rules.containsAll(rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destination);
    }
}
