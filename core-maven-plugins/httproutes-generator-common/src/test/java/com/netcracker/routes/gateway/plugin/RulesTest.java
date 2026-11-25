package com.netcracker.routes.gateway.plugin;

import com.netcracker.cloud.routesregistration.common.gateway.route.RouteEntry;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RulesTest {

    @Test
    void normalizesRepeatedSlashesAndTrailingSlash() {
        assertEquals("/api/x", Rules.normalize("api//x/"));
    }

    @Test
    void normalizesRoot() {
        assertEquals("/", Rules.normalize("/"));
        assertEquals("/", Rules.normalize(""));
    }

    @Test
    void templatedRouteWithoutRewrite() {
        SourcedRoute route = route("/api/v1/users/{id}", "/api/v1/users/{id}");
        Rule rule = Rules.toRule(route);
        assertEquals("/api/v1/users", rule.match());
        assertEquals(rule.match(), rule.target());
    }

    @Test
    void templatedRouteWithRewrite() {
        SourcedRoute route = route("/gw/{tenant}/items", "/svc/{id}/items");
        Rule rule = Rules.toRule(route);
        assertEquals("/gw", rule.match());
        assertEquals("/svc", rule.target());
    }

    @Test
    void inexpressibleRewriteFails() {
        SourcedRoute route = route("/gw/{id}/a", "/svc/{id}/b");
        RouteGenerationException ex = assertThrows(RouteGenerationException.class, () -> Rules.toRule(route));
        assertTrue(ex.getMessage().contains("/gw/{id}/a"));
    }

    @Test
    void rewriteTargetWithTemplateFails() {
        SourcedRoute route = route("/gw/{id}", "/svc/{id}/{other}");
        assertThrows(RouteGenerationException.class, () -> Rules.toRule(route));
    }

    @Test
    void checkNoPlaceholdersPassesWithoutPlaceholder() {
        Rules.checkNoPlaceholders(List.of(route("/api/users", "/api/users")));
    }

    @Test
    void checkNoPlaceholdersFailsNamingClassAndPlaceholder() {
        SourcedRoute route = new SourcedRoute("com.example.MyController",
                RouteEntry.builder().from("${api.prefix}/users").to("${api.prefix}/users").type(RouteType.PUBLIC).build());
        RouteGenerationException ex = assertThrows(RouteGenerationException.class,
                () -> Rules.checkNoPlaceholders(List.of(route)));
        assertTrue(ex.getMessage().contains("com.example.MyController"));
        assertTrue(ex.getMessage().contains("${api.prefix}"));
    }

    @Test
    void classNamesFiltersByPackageAndIncludesNestedClassesAndIgnoresNonClassFiles(@TempDir Path tempDir) throws IOException {
        createClassFile(tempDir, "com/netcracker/foo/MyController.class");
        createClassFile(tempDir, "com/netcracker/foo/MyController$Inner.class");
        createClassFile(tempDir, "com/other/OtherController.class");
        createNonClassFile(tempDir, "com/netcracker/foo/notes.txt");

        List<String> names = Rules.classNames(tempDir.toFile(), new String[]{"com.netcracker"});

        assertEquals(2, names.size());
        assertTrue(names.contains("com.netcracker.foo.MyController"));
        assertTrue(names.contains("com.netcracker.foo.MyController$Inner"));
    }

    @Test
    void classNamesReturnsEmptyForMissingDirectory() {
        List<String> names = Rules.classNames(new File("/does/not/exist"), new String[]{"com.netcracker"});
        assertTrue(names.isEmpty());
    }

    private void createClassFile(Path root, String relativePath) throws IOException {
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "stub");
    }

    private void createNonClassFile(Path root, String relativePath) throws IOException {
        createClassFile(root, relativePath);
    }

    private SourcedRoute route(String from, String to) {
        return new SourcedRoute("com.example.Controller",
                RouteEntry.builder().from(from).to(to).type(RouteType.PUBLIC).build());
    }
}
