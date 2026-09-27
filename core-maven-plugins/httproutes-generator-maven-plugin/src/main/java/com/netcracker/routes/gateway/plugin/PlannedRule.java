package com.netcracker.routes.gateway.plugin;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One planned HTTPRoute rule.
 *
 * @param resourceType HTTPRoute resource the rule goes into; {@code FACADE} is the service-bound HTTPRoute
 * @param matchType    path match type
 * @param value        path match value
 * @param rewriteType  URL rewrite filter type, or {@code null} for no filter
 * @param rewriteValue URL rewrite value, or {@code null} for no filter
 * @param timeout      request timeout in milliseconds, {@code 0} for none
 * @param sources      the declared routes the rule comes from
 */
public record PlannedRule(HttpRoute.Type resourceType, MatchType matchType, String value,
                          RewriteType rewriteType, String rewriteValue, long timeout, List<DeclaredRoute> sources) {

    public PlannedRule {
        Objects.requireNonNull(resourceType, "resourceType");
        Objects.requireNonNull(matchType, "matchType");
        Objects.requireNonNull(value, "value");
        if ((rewriteType == null) != (rewriteValue == null)) {
            throw new IllegalArgumentException("rewrite type and value must be set together");
        }
        sources = sources.stream()
                .sorted(Comparator.comparing((DeclaredRoute r) -> r.route().gatewayPath())
                        .thenComparing(r -> r.route().path())
                        .thenComparing(r -> r.route().type())
                        .thenComparingLong(r -> r.route().timeout())
                        .thenComparing(DeclaredRoute::originsText))
                .toList();
    }

    /**
     * @return the border gateways the rule is attached to; none for the service-bound HTTPRoute
     */
    public Set<Gateway> gateways() {
        return Gateway.exposedBy(resourceType);
    }

    public boolean hasRewrite() {
        return rewriteType != null;
    }

    /**
     * @return the prefix a {@code PathPrefix} rule matches, without the trailing slash Istio ignores
     */
    String prefixBase() {
        return prefixBase(value);
    }

    static String prefixBase(String prefix) {
        return prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
    }

    /**
     * @return whether the rule matches the request path, whole segments only for {@code PathPrefix}
     */
    public boolean matches(String requestPath) {
        return matchType == MatchType.EXACT ? value.equals(requestPath) : prefixMatches(value, requestPath);
    }

    /**
     * @return whether a {@code PathPrefix} match on {@code prefix} matches the request path, whole segments only
     */
    static boolean prefixMatches(String prefix, String requestPath) {
        String base = prefixBase(prefix);
        return base.isEmpty() || requestPath.equals(base) || requestPath.startsWith(base + "/");
    }

    /**
     * @return the path Istio sends upstream for a request path this rule matches
     */
    public String upstream(String requestPath) {
        if (rewriteType == null) {
            return requestPath;
        }
        if (rewriteType == RewriteType.REPLACE_FULL_PATH) {
            return rewriteValue;
        }
        return PathPattern.appendRemainder(rewriteValue, requestPath.substring(prefixBase().length()));
    }

    /**
     * @return for example {@code PathPrefix /api/v1/svc/resource -> /resource (from com.acme.C, com.acme.C#m)}
     */
    public String describe() {
        StringBuilder text = new StringBuilder(matchType.apiName()).append(' ').append(value);
        if (rewriteType != null) {
            text.append(" -> ").append(rewriteValue);
            if (rewriteType == RewriteType.REPLACE_FULL_PATH) {
                text.append(" (").append(rewriteType.apiName()).append(')');
            }
        }
        String origins = sources.stream().flatMap(r -> r.origins().stream()).distinct().sorted().collect(Collectors.joining(", "));
        return text.append(" (from ").append(origins).append(')').toString();
    }

    public enum MatchType {
        EXACT("Exact"),
        PATH_PREFIX("PathPrefix");

        private final String apiName;

        MatchType(String apiName) {
            this.apiName = apiName;
        }

        public String apiName() {
            return apiName;
        }
    }

    public enum RewriteType {
        REPLACE_PREFIX_MATCH("ReplacePrefixMatch"),
        REPLACE_FULL_PATH("ReplaceFullPath");

        private final String apiName;

        RewriteType(String apiName) {
            this.apiName = apiName;
        }

        public String apiName() {
            return apiName;
        }
    }
}
