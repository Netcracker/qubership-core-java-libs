package org.qubership.cloud.disableapi.quarkus;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class AntPathMatcher {

    public static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{[^:}]+(:([^}]+))?}");
    public static final Pattern PARAMETER_HOLDER_PATTERN = Pattern.compile("\\{#\\d+}");
    @Getter
    private final String antPath;
    @Getter
    private final Pattern antPathPattern;

    public AntPathMatcher(String antPath) {
        Objects.requireNonNull(antPath, "antPath cannot be null");
        this.antPath = antPath;
        this.antPathPattern = convert(antPath);
    }

    // The mapping matches URLs using the following rules:
    //? matches one character
    //* matches zero or more characters
    //** matches zero or more directories in a path
    //{spring:[a-z]+} matches the regexp [a-z]+ as a path variable named "spring"
    private Pattern convert(String antPath) {
        Map<String, String> paramsHolder = new HashMap<>();
        AtomicInteger counter = new AtomicInteger();
        // hide all params since they may contain regex chars like ?*+
        String regex = PARAMETER_PATTERN.matcher(antPath).replaceAll(result -> {
            String group = result.group(2);
            int id = counter.incrementAndGet();
            String key = "{#" + id + "}";
            if (group == null) {
                paramsHolder.put(key, "([^/]*)");
            } else {
                paramsHolder.put(key, "(" + group.replace("\\", "\\\\") + ")");
            }
            return key;
        });
        // replace all wildcards and special chars with regex
        regex = regex.replace("/", "\\/");
        regex = regex.replace("**", "{double-wildcard-holder}");
        regex = regex.replace("*", "([^/]*)");
        regex = regex.replace("{double-wildcard-holder}", "(.*)");
        regex = regex.replace("?", "([^/]{1})");
        // return hidden params
        regex = PARAMETER_HOLDER_PATTERN.matcher(regex).replaceAll(result -> {
            String group = result.group(0);
            return paramsHolder.get(group);
        });
        return Pattern.compile(regex);
    }

    boolean matches(String path) {
        return this.antPathPattern.matcher(path).matches();
    }

    @Override
    public String toString() {
        return "AntPathMatcher{'" + antPath + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AntPathMatcher that = (AntPathMatcher) o;
        return Objects.equals(antPath, that.antPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(antPath);
    }
}