package com.netcracker.routes.gateway.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathPatternTest {

    @Test
    void variableAsLastSegment() {
        PathPattern pattern = PathPattern.of("/a/{id}");

        assertEquals(List.of("a", "{id}"), pattern.segments());
        assertTrue(pattern.hasVariables());
        assertEquals(7, pattern.length());
        assertEquals(Optional.of(new PathPattern.Match(List.of("1"), "")), pattern.match("/a/1"));
        assertEquals(Optional.of(new PathPattern.Match(List.of("1"), "/")), pattern.match("/a/1/"));
        assertEquals(Optional.of(new PathPattern.Match(List.of("1"), "/x/y")), pattern.match("/a/1/x/y"));
        assertFalse(pattern.matches("/a"));
        assertFalse(pattern.matches("/a/"));
        assertFalse(pattern.matches("/b/1"));
        assertEquals("/a/{*}", pattern.istioTemplate());
    }

    @Test
    void variableInTheMiddle() {
        PathPattern pattern = PathPattern.of("/a/{id}/b");

        assertEquals(Optional.of(new PathPattern.Match(List.of("1"), "")), pattern.match("/a/1/b"));
        assertEquals(Optional.of(new PathPattern.Match(List.of("1"), "/c")), pattern.match("/a/1/b/c"));
        assertFalse(pattern.matches("/a/1"));
        assertFalse(pattern.matches("/a/1/bc"));
        assertFalse(pattern.matches("/a/1/2/b"));
        assertEquals("/a/x-probe-0/b", pattern.instantiate("x-probe-0"));
        assertEquals("/a/lit/b", pattern.substitute(List.of("lit")));
        assertEquals("/a/{*}/b", pattern.istioTemplate());
    }

    @Test
    void partialSegmentVariable() {
        PathPattern pattern = PathPattern.of("/files/{name}.txt");

        assertEquals(List.of("files", "{name}.txt"), pattern.segments());
        assertEquals(Optional.of(new PathPattern.Match(List.of("report"), "")), pattern.match("/files/report.txt"));
        assertFalse(pattern.matches("/files/report.csv"));
        assertFalse(pattern.matches("/files/.txt"));
        assertEquals("/files/x.txt", pattern.instantiate("x"));
        assertEquals("/files/{*}", pattern.istioTemplate());
        assertFalse(PathPattern.isVariable("{name}.txt"));
        assertTrue(PathPattern.hasVariable("{name}.txt"));
        assertTrue(PathPattern.isVariable("{name}"));
    }

    @Test
    void rootMatchesEverything() {
        PathPattern pattern = PathPattern.of("/");

        assertEquals(List.of(), pattern.segments());
        assertFalse(pattern.hasVariables());
        assertEquals(1, pattern.length());
        assertEquals(Optional.of(new PathPattern.Match(List.of(), "/")), pattern.match("/"));
        assertEquals(Optional.of(new PathPattern.Match(List.of(), "/a/b")), pattern.match("/a/b"));
        assertEquals("/", pattern.istioTemplate());
    }

    @Test
    void literalPatternMatchesWholeSegmentsAndTrailingSlash() {
        PathPattern pattern = PathPattern.of("/api/users");

        assertEquals(Optional.of(new PathPattern.Match(List.of(), "")), pattern.match("/api/users"));
        assertEquals(Optional.of(new PathPattern.Match(List.of(), "/")), pattern.match("/api/users/"));
        assertFalse(pattern.matches("/api/usersX"));
        assertFalse(pattern.matches("/api"));
    }

    @Test
    void patternWithTrailingSlashMatchesLikeWithout() {
        PathPattern pattern = PathPattern.of("/api/users/");

        assertEquals("/api/users/", pattern.source());
        assertEquals(11, pattern.length());
        assertTrue(pattern.matches("/api/users"));
        assertTrue(pattern.matches("/api/users/"));
        assertTrue(pattern.matches("/api/users/1"));
    }

    @Test
    void sourceIsNormalized() {
        assertEquals("/", PathPattern.of("").source());
        assertEquals("/", PathPattern.of(null).source());
        assertEquals("/a", PathPattern.of("a").source());
        assertEquals(PathPattern.of("/a"), PathPattern.of("a"));
    }

    @Test
    void substituteKeepsVariablesWithoutValue() {
        assertEquals("/one/1/{b}", PathPattern.of("/one/{a}/{b}").substitute(List.of("1")));
        assertEquals("/one/1/2", PathPattern.of("/one/{a}/{b}").substitute(List.of("1", "2")));
    }

    @Test
    void lengthCountsVariablesInSourceForm() {
        assertEquals("/s/{a}/b".length(), PathPattern.of("/s/{a}/b").length());
        assertEquals(PathPattern.of("/s/{a}/b").length(), PathPattern.of("/s/b/{a}").length());
    }

    @Test
    void cutAtFirstVariable() {
        assertEquals("/api/v1/my-service/resource", PathPattern.cut("/api/v1/my-service/resource/{var1}/internal-api/status"));
        assertEquals("/api/v1/my-service/order", PathPattern.cut("/api/v1/my-service/order/{id}"));
        assertEquals("/api/v1/files", PathPattern.cut("/api/v1/files/{name}.txt"));
        assertEquals("/api/v1/my-service/resource", PathPattern.cut("/api/v1/my-service/resource"));
        assertEquals("/", PathPattern.cut("/{id}"));
        assertEquals("/", PathPattern.cut("/{id}/items"));
        assertEquals("/a", PathPattern.cut("/a//{id}"));
    }

    @Test
    void cutDropsTrailingSlashOfPathWithoutVariables() {
        // Spring @GetMapping("/") under @RequestMapping("/api/items") must share the group of /api/items/{id}
        assertEquals("/api/items", PathPattern.cut("/api/items/"));
        assertEquals(PathPattern.cut("/api/items/{id}"), PathPattern.cut("/api/items/"));
        assertEquals("/", PathPattern.cut("/"));
    }

    @Test
    void istioTemplateDropsTrailingSlash() {
        assertEquals("/a/b", PathPattern.of("/a/b/").istioTemplate());
        assertEquals("/a/{*}", PathPattern.of("/a/{id}/").istioTemplate());
    }

    @Test
    void overlapsComparesSegmentsUpToTheShorterPattern() {
        assertTrue(PathPattern.of("/a/lit/x").overlaps(PathPattern.of("/a/{id}/x/y")));
        assertTrue(PathPattern.of("/a/{x}").overlaps(PathPattern.of("/{p}/bb")));
        assertTrue(PathPattern.of("/a/b/").overlaps(PathPattern.of("/a/b/c")));
        assertTrue(PathPattern.of("/files/{name}.txt").overlaps(PathPattern.of("/files/a.txt/meta")));
        assertFalse(PathPattern.of("/files/{name}.txt").overlaps(PathPattern.of("/files/a.csv")));
        assertFalse(PathPattern.of("/a/b").overlaps(PathPattern.of("/a/c/d")));
        assertFalse(PathPattern.of("/a/lit/x").overlaps(PathPattern.of("/a/{id}/z/y")));
    }

    @Test
    void forbiddenPathProblems() {
        assertEquals(Optional.empty(), PathPattern.forbiddenPathProblem("/a/{id}/b"));
        assertTrue(PathPattern.forbiddenPathProblem("/files/*").orElseThrow().contains("* wildcards"));
        assertTrue(PathPattern.forbiddenPathProblem("/items/{id:\\d+}").orElseThrow().contains("regex-constrained"));
        assertTrue(PathPattern.forbiddenPathProblem("/files/{name}.txt").orElseThrow().contains("whole-segment"));
    }
}
