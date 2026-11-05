package com.netcracker.routes.gateway.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helpers for gateway paths with {@code {name}} variables, where a variable matches one path segment.
 */
final class RoutePaths {

    private static final Pattern VARIABLE = Pattern.compile("\\{[^/{}]*}");
    /**
     * A literal that fills a variable in {@link #sample} and matches no literal of a route.
     */
    private static final String ANY = "~";

    private RoutePaths() {
    }

    /**
     * Cuts the path before the segment that contains the first variable, without the trailing slash.
     *
     * @return the cut path, or {@code /} if nothing remains
     */
    static String cut(String path) {
        int variable = path.indexOf('{');
        String prefix = variable < 0 ? path : path.substring(0, path.lastIndexOf('/', variable) + 1);
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix.isEmpty() ? "/" : prefix;
    }

    static boolean hasVariable(String path) {
        return path.contains("{");
    }

    static List<String> segments(String path) {
        return Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).toList();
    }

    /**
     * Compares the paths segment by segment, up to the shorter one: two variables always overlap,
     * a variable overlaps a literal it matches, and two literals overlap if they are equal.
     *
     * @return whether some request path matches both paths
     */
    static boolean overlaps(String a, String b) {
        List<String> left = segments(a);
        List<String> right = segments(b);
        for (int i = 0; i < Math.min(left.size(), right.size()); i++) {
            if (!segmentsOverlap(left.get(i), right.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return whether {@code route} has no more segments than {@code path} and overlaps it: for a request path, whether
     * the {@code PathPrefix} or legacy route {@code route} matches it; for a pattern, whether it matches some request
     * path that matches the pattern
     */
    static boolean covers(String route, String path) {
        return segments(route).size() <= segments(path).size() && overlaps(route, path);
    }

    private static boolean segmentsOverlap(String a, String b) {
        if (hasVariable(a) && hasVariable(b)) {
            return true;
        }
        if (hasVariable(a) || hasVariable(b)) {
            String variable = hasVariable(a) ? a : b;
            String literal = hasVariable(a) ? b : a;
            return segmentRegex(variable).matcher(literal).matches();
        }
        return a.equals(b);
    }

    private static Pattern segmentRegex(String segment) {
        StringBuilder regex = new StringBuilder();
        Matcher variables = VARIABLE.matcher(segment);
        int last = 0;
        while (variables.find()) {
            regex.append(Pattern.quote(segment.substring(last, variables.start()))).append("[^/]+");
            last = variables.end();
        }
        return Pattern.compile(regex.append(Pattern.quote(segment.substring(last))).toString());
    }

    /**
     * @return a request path that matches both overlapping paths, deep as the deeper one, with {@link #ANY} for
     * each variable that no literal of the other path fills
     */
    static String sample(String a, String b) {
        List<String> left = segments(a);
        List<String> right = segments(b);
        List<String> sample = new ArrayList<>();
        for (int i = 0; i < Math.max(left.size(), right.size()); i++) {
            String segment = i >= left.size() ? right.get(i)
                    : i >= right.size() || !hasVariable(left.get(i)) || VARIABLE.matcher(right.get(i)).matches()
                    ? left.get(i) : right.get(i);
            sample.add(VARIABLE.matcher(segment).replaceAll(ANY));
        }
        return sample.stream().collect(Collectors.joining("/", "/", ""));
    }

    /**
     * @return the path in AuthorizationPolicy form: each variable segment replaced with {@code {*}}, without the
     * trailing slash
     */
    static String template(String path) {
        return segments(path).stream()
                .map(s -> VARIABLE.matcher(s).matches() ? "{*}" : s)
                .collect(Collectors.joining("/", "/", ""));
    }

    /**
     * @return whether {@link #template} can express the path: every variable takes up a whole segment
     */
    static boolean expressible(String path) {
        return segments(path).stream().allMatch(s -> !hasVariable(s) || VARIABLE.matcher(s).matches());
    }
}
