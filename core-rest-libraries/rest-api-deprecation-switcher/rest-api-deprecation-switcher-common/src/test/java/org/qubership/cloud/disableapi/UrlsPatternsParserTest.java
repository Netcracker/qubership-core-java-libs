package org.qubership.cloud.disableapi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class UrlsPatternsParserTest {

    @Test
    void testPatternWithMethods() {
        test(patterns("/api/v1/** [POST GET DELETE]"),
                map(entry("/api/v1/**", "POST", "GET", "DELETE")));
    }

    @Test
    void testPatternWithSpacedMethods() {
        test(patterns(" /api/v1/** [ POST  GET  DELETE OPTIONS ] "),
                map(entry("/api/v1/**", "POST", "GET", "DELETE", "OPTIONS")));
    }

    @Test
    void testPatternWithEmptyMethods() {
        test(patterns(" /api/v1/** [  ] "), map(entry("/api/v1/**", "*")));
    }

    @Test
    void testPatternWithoutMethods() {
        test(patterns(" /api/v1/** "), map(entry("/api/v1/**", "*")));
    }

    @Test
    void testComplexPatternWithMethods() {
        test(patterns(" /path?/**/path4/{param1}*{param2}*{param3} [GET] "),
                map(entry("/path?/**/path4/{param1}*{param2}*{param3}", "GET")));
    }

    @Test
    void testComplexRegexPatternWithoutMethods() {
        test(patterns(" /path?/**/path4/{param1:[\\d+]}*{param2:([\\w+])}*{param3} "),
                map(entry("/path?/**/path4/{param1:[\\d+]}*{param2:([\\w+])}*{param3}", "*")));
    }

    private void test(List<String> patterns, Map<String, Set<String>> expectedMap) {
        Map<String, Set<String>> patternsMap = UrlsPatternsParser.parse(patterns);
        Assertions.assertEquals(expectedMap, patternsMap);
    }

    private List<String> patterns(String... patterns) {
        return Arrays.stream(patterns).collect(Collectors.toList());
    }

    private Map<String, Set<String>> map(Pair... entries) {
        return Arrays.stream(entries).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    private Pair entry(String key, String... values) {
        return new Pair(key, Arrays.stream(values).collect(Collectors.toSet()));
    }

    private static class Pair {
        private String key;
        private Set<String> value;

        public Pair(String key, Set<String> value) {
            this.key = key;
            this.value = value;
        }
        public String getKey() {
            return key;
        }
        public Set<String> getValue() {
            return value;
        }
    }
}
