package com.netcracker.routes.gateway.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A problem found while scanning, planning or validating routes.
 *
 * @param kind         what kind of problem it is
 * @param target       gateway name, {@link #SERVICE_TARGET}, several gateway names, or {@code null} for scan-time findings
 * @param details      labelled report lines, in report order
 * @param gatewayPaths gateway paths of the routes whose rewrites or precedence collide, empty for other findings;
 *                     {@link RouteMigration} drops the finding if one of them is {@code LEGACY_INVALID}
 */
public record Finding(Kind kind, String target, List<Detail> details, Set<String> gatewayPaths) {

    public static final String SERVICE_TARGET = "service-bound HTTPRoute";

    public Finding {
        details = List.copyOf(details);
        gatewayPaths = Set.copyOf(gatewayPaths);
    }

    public Finding(Kind kind, String target, List<Detail> details) {
        this(kind, target, details, Set.of());
    }

    public static Finding of(Kind kind, String target, String... labelTextPairs) {
        if (labelTextPairs.length % 2 != 0) {
            throw new IllegalArgumentException("labels and texts must come in pairs");
        }
        List<Detail> details = new ArrayList<>();
        for (int i = 0; i < labelTextPairs.length; i += 2) {
            details.add(new Detail(labelTextPairs[i], labelTextPairs[i + 1]));
        }
        return new Finding(kind, target, details);
    }

    /**
     * @return this finding about the routes with these gateway paths
     */
    public Finding about(Collection<String> gatewayPaths) {
        return new Finding(kind, target, details, Set.copyOf(gatewayPaths));
    }

    public boolean isError() {
        return kind.severity() == Severity.ERROR;
    }

    public Optional<String> detail(String label) {
        return details.stream().filter(d -> d.label().equals(label)).map(Detail::text).findFirst();
    }

    public enum Severity {
        ERROR, WARNING
    }

    public enum Kind {
        INVALID_FORBIDDEN_ROUTE(Severity.ERROR),
        LEGACY_INVALID(Severity.ERROR),
        CONTRADICTORY_DECLARATION(Severity.ERROR),
        LEGACY_PRECEDENCE_UNDEFINED(Severity.ERROR),
        CONFLICT(Severity.ERROR),
        EXPOSURE(Severity.ERROR),
        LOST_ROUTE(Severity.ERROR),
        TIMEOUT_CHANGE(Severity.WARNING),
        TIMEOUT_MERGE(Severity.WARNING),
        TIMEOUT_NOT_APPLIED(Severity.WARNING);

        private final Severity severity;

        Kind(Severity severity) {
            this.severity = severity;
        }

        public Severity severity() {
            return severity;
        }
    }

    public record Detail(String label, String text) {
    }
}
