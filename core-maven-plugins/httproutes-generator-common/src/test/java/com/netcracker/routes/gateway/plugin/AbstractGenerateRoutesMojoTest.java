package com.netcracker.routes.gateway.plugin;

import com.netcracker.cloud.routesregistration.common.gateway.route.RouteEntry;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractGenerateRoutesMojoTest {

    @Test
    void emptyOutputHasOnlyHeaderAndGuard(@TempDir Path tempDir) throws Exception {
        String content = run(tempDir, List.of());
        assertTrue(content.contains("THIS FILE WAS AUTOMATICALLY GENERATED"));
        assertTrue(content.contains("{{- if eq .Values.SERVICE_MESH_TYPE \"Istio\" }}"));
        assertTrue(content.contains("{{- end }}"));
        assertFalse(content.contains("HTTPRoute\n"));
    }

    @Test
    void oneGatewayAndOneFacadeRouteWrittenCorrectly(@TempDir Path tempDir) throws Exception {
        String content = run(tempDir, List.of(
                route("com.example.A", "/api", "/api", RouteType.PUBLIC, null),
                route("com.example.B", "/facade", "/svc", RouteType.FACADE, null)
        ));
        assertTrue(content.contains("java-annotations\""));
        assertTrue(content.contains("java-annotations-facade\""));
        assertTrue(content.contains("value: /api"));
        assertTrue(content.contains("value: /facade"));
    }

    @Test
    void publicPrivateInternalLandInGatewayGroup(@TempDir Path tempDir) throws Exception {
        String content = run(tempDir, List.of(
                route("com.example.A", "/pub", "/pub", RouteType.PUBLIC, null),
                route("com.example.B", "/priv", "/priv", RouteType.PRIVATE, null),
                route("com.example.C", "/int", "/int", RouteType.INTERNAL, null)
        ));
        assertTrue(content.contains("java-annotations\""));
        assertFalse(content.contains("java-annotations-facade"));
        assertTrue(content.contains("value: /pub"));
        assertTrue(content.contains("value: /priv"));
        assertTrue(content.contains("value: /int"));
    }

    @Test
    void facadeOnlyRoutesLandInFacadeGroup(@TempDir Path tempDir) throws Exception {
        String content = run(tempDir, List.of(route("com.example.A", "/facade", "/svc", RouteType.FACADE, null)));
        assertFalse(content.contains("java-annotations\""));
        assertTrue(content.contains("java-annotations-facade"));
    }

    @Test
    void facadeGroupWithOneRewriteRendersBoth(@TempDir Path tempDir) throws Exception {
        String content = run(tempDir, List.of(
                route("com.example.A", "/test/sleep", "/sleep", RouteType.FACADE, null),
                route("com.example.B", "/api/v1/plain", "/api/v1/plain", RouteType.FACADE, 10_000L)
        ));
        assertTrue(content.contains("value: /test/sleep"));
        assertTrue(content.contains("value: /api/v1/plain"));
    }

    @Test
    void facadeGroupWithOnlyPlainEntriesRendersNoFacadeHttpRoute(@TempDir Path tempDir) throws Exception {
        String content = run(tempDir, List.of(route("com.example.A", "/plain", "/plain", RouteType.FACADE, 10_000L)));
        assertFalse(content.contains("java-annotations-facade"));
    }

    @Test
    void rootSlashFailsNamingTheClass(@TempDir Path tempDir) {
        MojoExecutionException ex = assertThrows(MojoExecutionException.class,
                () -> run(tempDir, List.of(route("com.example.RootController", "/", "/", RouteType.PUBLIC, null))));
        assertTrue(ex.getMessage().contains("com.example.RootController"));
    }

    private SourcedRoute route(String className, String from, String to, RouteType type, Long timeout) {
        return new SourcedRoute(className, RouteEntry.builder().from(from).to(to).type(type).timeout(timeout).build());
    }

    private String run(Path tempDir, List<SourcedRoute> routes) throws Exception {
        StubMojo mojo = new StubMojo(routes);
        mojo.project = new MavenProject();
        mojo.project.setFile(tempDir.resolve("pom.xml").toFile());
        mojo.reactorProjects = List.of(mojo.project);
        mojo.outputFile = "target/httproutes-test/output.yaml";
        mojo.servicePort = 8080;
        mojo.backendRefVal = "{{ .Values.SERVICE_NAME }}";
        mojo.execute();
        return readOutput(tempDir);
    }

    private String readOutput(Path tempDir) throws IOException {
        return Files.readString(tempDir.resolve("target/httproutes-test/output.yaml"));
    }

    private static class StubMojo extends AbstractGenerateRoutesMojo {
        private final List<SourcedRoute> routes;

        StubMojo(List<SourcedRoute> routes) {
            this.routes = routes;
        }

        @Override
        protected List<SourcedRoute> collect(MavenProject module) {
            return routes;
        }

        @Override
        protected void assertFramework() {
        }
    }
}
