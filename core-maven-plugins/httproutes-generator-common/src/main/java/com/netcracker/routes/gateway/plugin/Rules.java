package com.netcracker.routes.gateway.plugin;

import com.netcracker.cloud.routesregistration.common.gateway.route.RouteEntry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class Rules {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{[^}]*}");
    private static final Pattern VARIABLE_SEGMENT = Pattern.compile("\\{[^/]*}");

    private Rules() {
    }

    public static String normalize(String path) {
        String withLeadingSlash = path.startsWith("/") ? path : "/" + path;
        String collapsed = withLeadingSlash.replaceAll("/+", "/");
        if (collapsed.length() > 1 && collapsed.endsWith("/")) {
            return collapsed.substring(0, collapsed.length() - 1);
        }
        return collapsed;
    }

    static String[] segments(String normalized) {
        if (normalized.equals("/")) {
            return new String[0];
        }
        return normalized.substring(1).split("/");
    }

    static String join(String[] segments, int fromInclusive, int toExclusive) {
        if (fromInclusive >= toExclusive) {
            return "/";
        }
        return "/" + String.join("/", Arrays.asList(segments).subList(fromInclusive, toExclusive));
    }

    private static boolean isVariable(String segment) {
        return segment.contains("{") || segment.contains("*");
    }

    private static String normalizeVariableNames(String[] segments, int fromInclusive, int toExclusive) {
        String joined = join(segments, fromInclusive, toExclusive);
        return VARIABLE_SEGMENT.matcher(joined).replaceAll("{}");
    }

    public static void checkNoPlaceholders(List<SourcedRoute> routes) {
        for (SourcedRoute route : routes) {
            checkNoPlaceholder(route.className(), route.entry().getFrom());
            checkNoPlaceholder(route.className(), route.entry().getTo());
        }
    }

    private static void checkNoPlaceholder(String className, String path) {
        if (path == null) {
            return;
        }
        var matcher = PLACEHOLDER.matcher(path);
        if (matcher.find()) {
            throw new RouteGenerationException(
                    "Class " + className + " has an unresolvable placeholder " + matcher.group() + " in route path " + path);
        }
    }

    public static Rule toRule(SourcedRoute sourcedRoute) {
        RouteEntry entry = sourcedRoute.entry();
        String className = sourcedRoute.className();
        String[] fromSegments = segments(normalize(entry.getFrom()));
        String[] toSegments = segments(normalize(entry.getTo()));

        int cutIndex = 0;
        while (cutIndex < fromSegments.length && !isVariable(fromSegments[cutIndex])) {
            cutIndex++;
        }

        String match = join(fromSegments, 0, cutIndex);
        String target;

        int tailLength = fromSegments.length - cutIndex;
        if (tailLength > 0) {
            if (toSegments.length < tailLength) {
                throw new RouteGenerationException(
                        "Route " + entry.getFrom() + " -> " + entry.getTo() + " from class " + className
                                + " cannot be expressed as a prefix rewrite: target is shorter than the variable tail");
            }
            int toTailStart = toSegments.length - tailLength;
            String normalizedFromTail = normalizeVariableNames(fromSegments, cutIndex, fromSegments.length);
            String normalizedToTail = normalizeVariableNames(toSegments, toTailStart, toSegments.length);
            if (!normalizedFromTail.equals(normalizedToTail)) {
                throw new RouteGenerationException(
                        "Route " + entry.getFrom() + " -> " + entry.getTo() + " from class " + className
                                + " cannot be expressed as a prefix rewrite: tails differ");
            }
            target = join(toSegments, 0, toTailStart);
        } else {
            target = normalize(entry.getTo());
        }

        if (isVariable(target)) {
            throw new RouteGenerationException(
                    "Route " + entry.getFrom() + " -> " + entry.getTo() + " from class " + className
                            + " cannot be expressed as a prefix rewrite: rewrite target contains a template");
        }

        long timeout = entry.getTimeout() != null && entry.getTimeout() > 0 ? entry.getTimeout() : 0L;
        return new Rule(match, target, timeout, Set.of(className));
    }

    public static List<String> classNames(File outputDirectory, String[] packages) {
        List<String> result = new ArrayList<>();
        Path root = outputDirectory.toPath();
        if (!Files.isDirectory(root)) {
            return result;
        }
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".class"))
                    .forEach(p -> {
                        String relative = root.relativize(p).toString();
                        String className = relative.substring(0, relative.length() - ".class".length())
                                .replace(File.separatorChar, '.');
                        if (isInPackages(className, packages)) {
                            result.add(className);
                        }
                    });
        } catch (IOException e) {
            throw new RouteGenerationException("Failed to walk output directory " + outputDirectory + ": " + e.getMessage());
        }
        return result;
    }

    private static boolean isInPackages(String className, String[] packages) {
        int lastDot = className.lastIndexOf('.');
        String packageName = lastDot >= 0 ? className.substring(0, lastDot) : "";
        for (String pkg : packages) {
            if (packageName.equals(pkg) || packageName.startsWith(pkg + ".")) {
                return true;
            }
        }
        return false;
    }
}
