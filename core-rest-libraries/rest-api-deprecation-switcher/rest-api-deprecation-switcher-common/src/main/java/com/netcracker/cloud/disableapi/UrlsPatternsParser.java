package com.netcracker.cloud.disableapi;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UrlsPatternsParser {
    public static final String WILDCARD = "*";
    public static final Pattern PATH_WITH_METHODS_PATTERN = Pattern.compile("\\s*(\\S+)\\s*(\\[([^\\[\\]]+)]\\s*)?");
    public static final Pattern DELIM_PATTERN = Pattern.compile("[\\s,]+");

    private UrlsPatternsParser() {
    }

    public static Map<String, Set<String>> parse(Collection<String> patterns) {
        return patterns.stream().map(path -> {
            Matcher matcher = PATH_WITH_METHODS_PATTERN.matcher(path);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(String.format("Invalid path '%s'. Valid pattern = '%s'", path,
                        PATH_WITH_METHODS_PATTERN.pattern()));
            }
            return new AbstractMap.SimpleEntry<>(path, matcher);
        }).collect(Collectors.toMap(entry -> entry.getValue().group(1).trim(), entry -> {
            String methodsGroup = entry.getValue().group(3);
            if (methodsGroup == null || methodsGroup.isBlank()) {
                return Stream.of(WILDCARD).collect(Collectors.toSet());
            } else {
                return Arrays.stream(DELIM_PATTERN.matcher(methodsGroup.trim()).replaceAll(",").split(","))
                        .collect(Collectors.toSet());
            }
        }, (one, another) -> Stream.concat(one.stream(), another.stream()).collect(Collectors.toSet())));
    }
}
