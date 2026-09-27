package com.netcracker.routes.gateway.plugin;

import org.apache.maven.plugin.logging.Log;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Formats findings for the build log, grouped by gateway (design D11).
 */
public final class FindingReport {

    static final String PREFIX = "[ROUTE-MIGRATION]";

    private FindingReport() {
    }

    /**
     * @return findings in report order: scan-time findings, then public, private and internal gateway,
     * then the service-bound HTTPRoute; the original order is kept within a group
     */
    public static List<Finding> sorted(Collection<Finding> findings) {
        return findings.stream().sorted(Comparator.comparingInt(FindingReport::rank)).toList();
    }

    private static int rank(Finding finding) {
        String target = finding.target();
        if (target == null) {
            return 0;
        }
        if (target.equals(Finding.SERVICE_TARGET)) {
            return Gateway.values().length + 1;
        }
        for (Gateway gateway : Gateway.values()) {
            if (target.startsWith(gateway.refName())) {
                return gateway.ordinal() + 1;
            }
        }
        return Gateway.values().length + 1;
    }

    /**
     * @return for example {@code [ROUTE-MIGRATION] EXPOSURE on public-gateway} followed by indented {@code label: text} lines
     */
    public static String format(Finding finding) {
        StringBuilder text = new StringBuilder(PREFIX).append(' ').append(finding.kind());
        if (finding.target() != null) {
            text.append(" on ").append(finding.target());
        }
        int width = finding.details().stream().mapToInt(d -> d.label().length()).max().orElse(0) + 1;
        for (Finding.Detail detail : finding.details()) {
            String label = detail.label() + ":";
            text.append(System.lineSeparator()).append("    ").append(label)
                    .append(" ".repeat(width - label.length() + 1)).append(detail.text());
        }
        return text.toString();
    }

    /**
     * Logs all findings in report order: errors as errors, warnings as warnings.
     */
    public static void log(Log log, Collection<Finding> findings) {
        for (Finding finding : sorted(findings)) {
            if (finding.isError()) {
                log.error(format(finding));
            } else {
                log.warn(format(finding));
            }
        }
    }

    /**
     * @return the build failure message with the error counts by kind, for example
     * {@code Route migration validation failed with 3 errors (1 CONFLICT, 2 EXPOSURE), see the [ROUTE-MIGRATION] errors in the build log}
     */
    public static String summary(Collection<Finding> findings) {
        Map<Finding.Kind, Long> counts = new EnumMap<>(Finding.Kind.class);
        findings.stream().filter(Finding::isError).forEach(f -> counts.merge(f.kind(), 1L, Long::sum));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        String byKind = counts.entrySet().stream().map(e -> e.getValue() + " " + e.getKey()).collect(Collectors.joining(", "));
        return "Route migration validation failed with " + total + (total == 1 ? " error" : " errors") + " (" + byKind
                + "), see the " + PREFIX + " errors in the build log";
    }
}
