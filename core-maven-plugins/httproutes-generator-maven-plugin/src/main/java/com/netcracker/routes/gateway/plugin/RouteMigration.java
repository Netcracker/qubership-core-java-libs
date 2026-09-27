package com.netcracker.routes.gateway.plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The pipeline after scanning (design D1): legacy model, Istio plan and validation.
 *
 * @param findings the findings of all stages: scan, legacy model, planner and validator
 */
public record RouteMigration(RouteDeclarations declarations, LegacyRouteTable legacy, IstioPlan plan, List<Finding> findings) {

    public RouteMigration {
        findings = List.copyOf(findings);
    }

    public static RouteMigration run(RouteDeclarations declarations, boolean autoGenerateAuthorizationPolicies) {
        LegacyRouteTable legacy = LegacyRouteTable.build(declarations);
        IstioPlan plan = new IstioRoutePlanner(autoGenerateAuthorizationPolicies).plan(declarations, legacy);
        List<Finding> findings = new ArrayList<>(declarations.findings());
        findings.addAll(legacy.findings());
        findings.addAll(plan.findings());
        findings.addAll(new MigrationValidator(autoGenerateAuthorizationPolicies).validate(declarations, legacy, plan));
        Set<String> legacyInvalid = legacyInvalidGatewayPaths(legacy);
        findings.removeIf(f -> f.gatewayPaths().stream().anyMatch(legacyInvalid::contains));
        return new RouteMigration(declarations, legacy, plan, findings);
    }

    /**
     * Collisions of a route whose gateway path is {@code LEGACY_INVALID} aren't reported: that error fails the build
     * already, and legacy has no defined behavior to compare the route with.
     */
    private static Set<String> legacyInvalidGatewayPaths(LegacyRouteTable legacy) {
        Set<String> paths = new HashSet<>();
        for (Gateway gateway : Gateway.values()) {
            legacy.legacyInvalidPatterns(gateway).forEach(p -> paths.add(p.source()));
        }
        return paths;
    }

    public boolean hasErrors() {
        return findings.stream().anyMatch(Finding::isError);
    }
}
