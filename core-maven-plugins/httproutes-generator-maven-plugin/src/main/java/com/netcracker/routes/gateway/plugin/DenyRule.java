package com.netcracker.routes.gateway.plugin;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * One rule of a gateway's DENY AuthorizationPolicy.
 *
 * @param gateway  the border gateway the policy targets
 * @param paths    path templates the rule denies
 * @param notPaths path templates excluded from the rule, may be empty
 * @param origin   the {@code @ForbiddenRoute} element, or a synthetic {@code auto: ...} origin for automatic rules
 */
public record DenyRule(Gateway gateway, List<String> paths, List<String> notPaths, String origin) {

    public static final String AUTO_ORIGIN_PREFIX = "auto: ";

    public DenyRule {
        Objects.requireNonNull(gateway, "gateway");
        paths = List.copyOf(paths);
        notPaths = List.copyOf(notPaths);
    }

    public boolean isAutomatic() {
        return origin.startsWith(AUTO_ORIGIN_PREFIX);
    }

    /**
     * @return whether Istio denies the request path: it matches {@code paths} and doesn't match {@code notPaths}
     */
    public boolean denies(String requestPath) {
        return paths.stream().anyMatch(t -> templateMatches(t, requestPath))
                && notPaths.stream().noneMatch(t -> templateMatches(t, requestPath));
    }

    /**
     * @return the template for a path and the template for everything below it: {@code [T, T/{**}]}, {@code ["/", "/{**}"]} for the root
     */
    public static List<String> pathAndSubtree(String template) {
        return template.equals("/") ? List.of("/", "/{**}") : List.of(template, template + "/{**}");
    }

    /**
     * Istio path template matching: {@code {*}} matches one segment, {@code {**}} (last only) zero or more segments.
     */
    static boolean templateMatches(String template, String requestPath) {
        StringBuilder regex = new StringBuilder();
        String[] parts = template.split("/", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                regex.append('/');
            }
            switch (parts[i]) {
                case "{*}" -> regex.append("[^/]+");
                case "{**}" -> regex.append(".*");
                default -> regex.append(Pattern.quote(parts[i]));
            }
        }
        return Pattern.matches(regex.toString(), requestPath);
    }

    public String describe() {
        String text = "DENY " + String.join(", ", paths);
        if (!notPaths.isEmpty()) {
            text += " except " + String.join(", ", notPaths);
        }
        return text + " (from " + origin + ")";
    }
}
