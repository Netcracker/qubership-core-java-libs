package com.netcracker.routes.gateway.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A gateway or service path with {@code {name}} variables, matched the way the legacy gateways match it:
 * a variable matches one non-empty segment, and the pattern matches a request equal to it or continuing it with {@code /}.
 */
public final class PathPattern {

    private static final Pattern VARIABLE = Pattern.compile("\\{[^/{}]*}");
    private static final Pattern REGEX_CONSTRAINED_VARIABLE = Pattern.compile("\\{[^/}]*:");
    private static final String VARIABLE_REGEX = "([^/]+)";

    private final String source;
    private final List<String> segments;
    private final Pattern matcher;
    private final int variableCount;

    private PathPattern(String source) {
        this.source = normalize(source);
        this.segments = splitSegments(this.source);
        String body = this.source.endsWith("/") ? this.source.substring(0, this.source.length() - 1) : this.source;
        StringBuilder regex = new StringBuilder();
        Matcher variables = VARIABLE.matcher(body);
        int last = 0;
        int count = 0;
        while (variables.find()) {
            regex.append(Pattern.quote(body.substring(last, variables.start()))).append(VARIABLE_REGEX);
            last = variables.end();
            count++;
        }
        regex.append(Pattern.quote(body.substring(last))).append("(/.*)?");
        this.matcher = Pattern.compile(regex.toString());
        this.variableCount = count;
    }

    public static PathPattern of(String source) {
        return new PathPattern(source);
    }

    public String source() {
        return source;
    }

    /**
     * @return length of the source text, variables counted in their {@code {name}} form (legacy precedence)
     */
    public int length() {
        return source.length();
    }

    public List<String> segments() {
        return segments;
    }

    public boolean hasVariables() {
        return variableCount > 0;
    }

    public Optional<Match> match(String requestPath) {
        Matcher m = matcher.matcher(requestPath);
        if (!m.matches()) {
            return Optional.empty();
        }
        List<String> captures = new ArrayList<>(variableCount);
        for (int i = 1; i <= variableCount; i++) {
            captures.add(m.group(i));
        }
        String remainder = m.group(variableCount + 1);
        return Optional.of(new Match(captures, remainder == null ? "" : remainder));
    }

    public boolean matches(String requestPath) {
        return matcher.matcher(requestPath).matches();
    }

    /**
     * @return the source with every variable replaced by {@code sample}
     */
    public String instantiate(String sample) {
        return VARIABLE.matcher(source).replaceAll(Matcher.quoteReplacement(sample));
    }

    /**
     * @return the source with its variables replaced by position with {@code values}; variables without a value are kept
     */
    public String substitute(List<String> values) {
        Matcher variables = VARIABLE.matcher(source);
        StringBuilder result = new StringBuilder();
        int index = 0;
        while (variables.find()) {
            String replacement = index < values.size() ? values.get(index) : variables.group();
            variables.appendReplacement(result, Matcher.quoteReplacement(replacement));
            index++;
        }
        variables.appendTail(result);
        return result.toString();
    }

    /**
     * @return the source as an Istio AuthorizationPolicy path template: every segment with a variable becomes {@code {*}},
     * and the trailing slash is dropped, because the pattern matches the path without it too
     */
    public String istioTemplate() {
        List<String> matched = matchSegments();
        if (matched.isEmpty()) {
            return "/";
        }
        StringBuilder template = new StringBuilder();
        for (String segment : matched) {
            template.append('/').append(hasVariable(segment) ? "{*}" : segment);
        }
        return template.toString();
    }

    /**
     * Compares the patterns segment by segment, up to the shorter one: a literal overlaps an equal literal
     * and a variable segment that matches it, and two variable segments always overlap.
     *
     * @return whether some request path matches both patterns
     */
    public boolean overlaps(PathPattern other) {
        List<String> mine = matchSegments();
        List<String> theirs = other.matchSegments();
        for (int i = 0; i < Math.min(mine.size(), theirs.size()); i++) {
            if (!segmentsOverlap(mine.get(i), theirs.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean segmentsOverlap(String a, String b) {
        if (hasVariable(a) && hasVariable(b)) {
            return true;
        }
        if (hasVariable(a) || hasVariable(b)) {
            String variable = hasVariable(a) ? a : b;
            String literal = hasVariable(a) ? b : a;
            return of("/" + variable).matches("/" + literal);
        }
        return a.equals(b);
    }

    /**
     * @return the segments without the empty segment of a trailing {@code /}, which matching ignores
     */
    List<String> matchSegments() {
        return !segments.isEmpty() && segments.get(segments.size() - 1).isEmpty()
                ? segments.subList(0, segments.size() - 1) : segments;
    }

    public static boolean hasVariable(String segment) {
        return segment.contains("{");
    }

    /**
     * @return whether the segment is exactly one variable, such as {@code {id}}
     */
    public static boolean isVariable(String segment) {
        return VARIABLE.matcher(segment).matches();
    }

    /**
     * Cuts the path before the segment that contains the first variable, without the trailing slash.
     * Returns {@code /} if nothing remains, and the path without its trailing slash if it has no variables:
     * legacy matches {@code /a/} and {@code /a} the same way, and Istio ignores the trailing slash of a {@code PathPrefix}.
     */
    public static String cut(String path) {
        String normalized = normalize(path);
        int variable = normalized.indexOf('{');
        String prefix = variable < 0 ? normalized : normalized.substring(0, normalized.lastIndexOf('/', variable));
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix.isEmpty() ? "/" : prefix;
    }

    /**
     * @return why the path can't be a forbidden path, or empty if it can
     */
    public static Optional<String> forbiddenPathProblem(String path) {
        if (path.contains("*")) {
            return Optional.of("forbidden paths must not contain * wildcards");
        }
        if (REGEX_CONSTRAINED_VARIABLE.matcher(path).find()) {
            return Optional.of("forbidden paths must not contain regex-constrained variables such as {id:\\d+}");
        }
        boolean partial = splitSegments(normalize(path)).stream()
                .anyMatch(segment -> hasVariable(segment) && !isVariable(segment));
        if (partial) {
            return Optional.of("forbidden paths need whole-segment variables, a variable must take up a whole path segment");
        }
        return Optional.empty();
    }

    /**
     * Appends the unmatched rest of a request path to a rewritten path, the way both meshes join them:
     * the trailing slash of {@code rewritten} is dropped, and an empty result becomes {@code /}.
     */
    static String appendRemainder(String rewritten, String remainder) {
        String base = rewritten.endsWith("/") ? rewritten.substring(0, rewritten.length() - 1) : rewritten;
        String result = base + remainder;
        return result.isEmpty() ? "/" : result;
    }

    static String normalize(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static List<String> splitSegments(String source) {
        if (source.equals("/")) {
            return List.of();
        }
        return List.copyOf(Arrays.asList(source.substring(1).split("/", -1)));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PathPattern other && source.equals(other.source);
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }

    @Override
    public String toString() {
        return source;
    }

    /**
     * @param captures  values of the variables, in order
     * @param remainder the part of the request after the pattern, empty or starting with {@code /}
     */
    public record Match(List<String> captures, String remainder) {
    }
}
