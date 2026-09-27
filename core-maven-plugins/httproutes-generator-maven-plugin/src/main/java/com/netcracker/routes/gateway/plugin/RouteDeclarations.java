package com.netcracker.routes.gateway.plugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * Everything the scanner found.
 *
 * @param routes              declared routes, sorted by path specificity
 * @param forbidden           {@code @ForbiddenRoute} declarations
 * @param findings            scan-time findings
 * @param elementGatewayPaths border gateway path → classes ({@code class}) and methods ({@code class#method}) mapped to it,
 *                            used to suggest where to put {@code @ForbiddenRoute}
 */
public record RouteDeclarations(List<DeclaredRoute> routes,
                                List<ForbiddenDeclaration> forbidden,
                                List<Finding> findings,
                                Map<String, SortedSet<String>> elementGatewayPaths) {

    public RouteDeclarations {
        routes = List.copyOf(routes);
        forbidden = List.copyOf(forbidden);
        findings = List.copyOf(findings);
        elementGatewayPaths = Collections.unmodifiableMap(new TreeMap<>(elementGatewayPaths));
    }

    public RouteDeclarations(List<DeclaredRoute> routes, List<ForbiddenDeclaration> forbidden) {
        this(routes, forbidden, List.of(), Map.of());
    }
}
