package com.netcracker.routes.gateway.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Request paths on which the legacy and Istio models are compared (design D3).
 */
public final class Probes {

    static final String SAMPLE_PREFIX = "x-probe-";

    private Probes() {
    }

    /**
     * @return {@code x-probe-N} with the smallest N that is not a segment of any of the paths
     */
    public static String sample(Collection<String> paths) {
        Set<String> segments = new HashSet<>();
        for (String path : paths) {
            segments.addAll(PathPattern.of(path).segments());
        }
        int n = 0;
        while (segments.contains(SAMPLE_PREFIX + n)) {
            n++;
        }
        return SAMPLE_PREFIX + n;
    }

    /**
     * @return the sample for all gateway paths, service paths and forbidden paths of the declarations
     */
    public static String sample(RouteDeclarations declarations) {
        return sample(Stream.concat(
                declarations.routes().stream().flatMap(r -> Stream.of(r.route().gatewayPath(), r.route().path())),
                declarations.forbidden().stream().map(ForbiddenDeclaration::gatewayPath)).toList());
    }

    /**
     * @return {@code path/segment}, or {@code /segment} for the root
     */
    static String child(String path, String segment) {
        return (path.endsWith("/") ? path : path + "/") + segment;
    }

    /**
     * @return probes for the border gateway paths, forbidden paths and border match values of the plan
     */
    public static SortedSet<String> probes(RouteDeclarations declarations, IstioPlan plan) {
        List<String> patterns = new ArrayList<>();
        declarations.routes().stream().filter(r -> r.route().type() != HttpRoute.Type.FACADE)
                .forEach(r -> patterns.add(r.route().gatewayPath()));
        declarations.forbidden().forEach(f -> patterns.add(f.gatewayPath()));
        plan.rules().stream().filter(r -> r.resourceType() != HttpRoute.Type.FACADE).forEach(r -> patterns.add(r.value()));
        plan.conflicted().stream().filter(c -> c.resourceType() != HttpRoute.Type.FACADE).forEach(c -> patterns.add(c.value()));
        return probes(patterns, plan.sample());
    }

    /**
     * Request paths on which legacy and Istio decisions can differ (design D3). For every segment prefix of every pattern:
     * the prefix, the prefix with {@code /}, and the prefix with one more {@code sample} segment. Variables are
     * instantiated with {@code sample}, and, one variable at a time, with every literal segment that another pattern
     * has at the same position under a compatible prefix.
     */
    public static SortedSet<String> probes(Collection<String> patterns, String sample) {
        List<List<String>> segmentLists = patterns.stream().map(Probes::segments).distinct().toList();
        SortedSet<String> probes = new TreeSet<>();
        for (List<String> segments : segmentLists) {
            for (int k = 0; k <= segments.size(); k++) {
                List<String> prefix = segments.subList(0, k);
                List<String> base = prefix.stream().map(s -> instantiateSegment(s, sample)).toList();
                addProbes(probes, base, sample);
                for (int i = 0; i < k; i++) {
                    if (!PathPattern.hasVariable(prefix.get(i))) {
                        continue;
                    }
                    for (String literal : literalsAt(segmentLists, prefix, i)) {
                        List<String> instance = new ArrayList<>(base);
                        instance.set(i, literal);
                        addProbes(probes, instance, sample);
                    }
                }
            }
        }
        return probes;
    }

    private static void addProbes(Set<String> probes, List<String> segments, String sample) {
        String path = "/" + String.join("/", segments);
        probes.add(path);
        if (!path.equals("/")) {
            probes.add(path + "/");
        }
        probes.add(child(path, sample));
    }

    /**
     * @return literal segments at position {@code i} of other patterns whose earlier segments are compatible with
     * {@code prefix}, and which the variable segment {@code prefix[i]} matches
     */
    private static Set<String> literalsAt(List<List<String>> segmentLists, List<String> prefix, int i) {
        PathPattern variable = PathPattern.of("/" + prefix.get(i));
        Set<String> literals = new TreeSet<>();
        for (List<String> other : segmentLists) {
            if (other.size() <= i || PathPattern.hasVariable(other.get(i)) || !variable.matches("/" + other.get(i))) {
                continue;
            }
            boolean compatible = true;
            for (int j = 0; j < i && compatible; j++) {
                compatible = prefix.get(j).equals(other.get(j))
                        || PathPattern.hasVariable(prefix.get(j)) || PathPattern.hasVariable(other.get(j));
            }
            if (compatible) {
                literals.add(other.get(i));
            }
        }
        return literals;
    }

    /**
     * @return the segments of a pattern without the empty segment of a trailing {@code /}
     */
    private static List<String> segments(String pattern) {
        return PathPattern.of(pattern).matchSegments();
    }

    private static String instantiateSegment(String segment, String sample) {
        return PathPattern.of("/" + segment).instantiate(sample).substring(1);
    }
}
