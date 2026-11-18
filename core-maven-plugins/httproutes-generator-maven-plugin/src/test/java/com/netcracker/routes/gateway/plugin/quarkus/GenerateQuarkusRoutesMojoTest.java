package com.netcracker.routes.gateway.plugin.quarkus;

import com.netcracker.cloud.quarkus.routesregistration.deployment.RoutesRegistrationProcessor;
import com.netcracker.cloud.routesregistration.common.annotation.processing.RouteHostMapping;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteEntry;
import com.netcracker.routes.gateway.plugin.FrameworkMismatch;
import com.netcracker.routes.gateway.plugin.SourcedRoute;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateQuarkusRoutesMojoTest {

    private static final File TEST_CLASSES = new File("target/test-classes").getAbsoluteFile();

    @Test
    void mainFixturesProduceExpectedReferenceRoots(@TempDir Path tempDir) throws Exception {
        String yaml = render(tempDir, "com.netcracker.routes.gateway.plugin");

        // Quarkus1 -> /api/v1/test1 at 250s
        assertTrue(yaml.contains("value: /api/v1/test1"));
        assertTrue(yaml.contains("request: 250s"));

        // Quarkus2 -> /test1
        assertTrue(yaml.contains("value: /test1"));

        // Quarkus3 -> /class/path/from -> /class/path/to, plus nested method roots
        assertTrue(yaml.contains("value: /class/path/from"));
        assertTrue(yaml.contains("replacePrefixMatch: /class/path/to"));
        assertTrue(yaml.contains("value: /class/path/from/method/path/from"));
        assertTrue(yaml.contains("value: /class/path/from/method/path/from2"));
        assertTrue(yaml.contains("replacePrefixMatch: /class/path/to/method/path/to"));

        // Quarkus4 -> /test/sleep -> /sleep
        assertTrue(yaml.contains("value: /test/sleep"));
        assertTrue(yaml.contains("replacePrefixMatch: /sleep"));
    }

    @Test
    void interfacePathWinsOverSuperclassPath(@TempDir Path tempDir) throws Exception {
        String yaml = render(tempDir, "com.netcracker.cloud.quarkus.routesregistration.deployment.resource.hierarchy");
        assertTrue(yaml.contains("value: /x-icase1"));
        assertFalse(yaml.contains("/x-case3"));
    }

    @Test
    void undeclaredSubclassIsNotCandidate(@TempDir Path tempDir) throws Exception {
        String yaml = render(tempDir, "com.netcracker.routes.gateway.plugin");
        assertTrue(yaml.contains("value: /base"));
    }

    @Test
    void gatewayRequestMappingIgnoredForQuarkus(@TempDir Path tempDir) throws Exception {
        String yaml = render(tempDir, "com.netcracker.routes.gateway.plugin");
        assertTrue(yaml.contains("value: /svc-ignored"));
        assertFalse(yaml.contains("/gw-ignored"));
    }

    @Test
    void facadeGatewayAloneRendersRewrittenFacadeRoot() throws Exception {
        String yaml = renderIsolated("com.netcracker.quarkusfixtures.facade");
        assertTrue(yaml.contains("java-annotations-facade"));
        assertTrue(yaml.contains("value: /test/sleep"));
        assertTrue(yaml.contains("replacePrefixMatch: /sleep"));
    }

    @Test
    void facadeGatewayCombinedWithPlainFacadeRendersBoth() throws Exception {
        String yaml = renderIsolated(
                "com.netcracker.quarkusfixtures.facade",
                "com.netcracker.quarkusfixtures.facadetimeout");
        assertTrue(yaml.contains("java-annotations-facade"));
        assertTrue(yaml.contains("value: /test/sleep"));
        assertTrue(yaml.contains("value: /api/v1/reports"));
    }

    @Test
    void facadeRootWithLongTimeoutRendersAlone() throws Exception {
        String yaml = renderIsolated("com.netcracker.quarkusfixtures.facadetimeout");
        assertTrue(yaml.contains("java-annotations-facade"));
        assertTrue(yaml.contains("request: 5m"));
    }

    @Test
    void springGuardFailsWithExplicitMessage() {
        MojoExecutionException ex = assertThrows(MojoExecutionException.class,
                () -> renderIsolated("com.netcracker.quarkusfixtures.springguard"));
        assertEquals(FrameworkMismatch.QUARKUS_NOT_FOUND_MESSAGE, ex.getMessage());
    }

    @Test
    void springWebMixedDoesNotFailFrameworkMismatch(@TempDir Path tempDir) throws Exception {
        render(tempDir, "com.netcracker.quarkusfixtures.springwebmixed");
    }

    @Test
    void reactorWideSpringModuleAndQuarkusModuleDoesNotFail(@TempDir Path tempDir) throws Exception {
        Path springOut = tempDir.resolve("spring-classes");
        Path quarkusOut = tempDir.resolve("quarkus-classes");
        copyPackage(springOut, "com.netcracker.quarkusfixtures.springguard");
        copyPackage(quarkusOut, "com.netcracker.routes.gateway.plugin");

        GenerateQuarkusRoutesMojo mojo = new GenerateQuarkusRoutesMojo();
        MavenProject executing = testProjectWithBasedir(tempDir);
        setField(mojo, "reactorProjects", List.of(moduleProject(springOut), moduleProject(quarkusOut)));
        setField(mojo, "project", executing);
        setField(mojo, "packages", new String[]{
                "com.netcracker.quarkusfixtures.springguard",
                "com.netcracker.routes.gateway.plugin"
        });
        setField(mojo, "servicePort", 8080);
        setField(mojo, "backendRefVal", "{{ .Values.SERVICE_NAME }}");
        setField(mojo, "outputFile", "output.yaml");
        mojo.execute();

        String yaml = Files.readString(tempDir.resolve("output.yaml"));
        assertTrue(yaml.contains("value: /api/v1/test1"));
    }

    @Test
    void otherFixturesExcludeSpringGuardPackage(@TempDir Path tempDir) throws Exception {
        render(tempDir, "com.netcracker.routes.gateway.plugin");
    }

    @Test
    void collectedRoutesMatchLibraryExactly() throws Exception {
        String pkg = "com.netcracker.routes.gateway.plugin";
        GenerateQuarkusRoutesMojo mojo = new GenerateQuarkusRoutesMojo();
        setField(mojo, "packages", new String[]{pkg});
        List<SourcedRoute> collected = mojo.collect(testProject());

        IndexView index = buildTestIndex();
        RoutesRegistrationProcessor processor = new RoutesRegistrationProcessor();
        Map<String, ClassInfo> classesWithRoutes = processor.collectClassesWithRoutes(index);
        Set<String> expectedCandidates = classesWithRoutes.keySet().stream()
                .filter(name -> name.equals(pkg) || name.startsWith(pkg + "."))
                .collect(Collectors.toSet());

        Set<String> actualCandidates = collected.stream().map(SourcedRoute::className).collect(Collectors.toSet());
        assertEquals(expectedCandidates, actualCandidates, "Candidate class set differs from an independent scan");

        Map<String, List<SourcedRoute>> actualByClass = collected.stream().collect(Collectors.groupingBy(SourcedRoute::className));
        for (String className : expectedCandidates) {
            ClassInfo classInfo = classesWithRoutes.get(className);
            List<RouteEntry> expected = processor.collectRoutesForClass(index, classInfo, "{{ .Values.SERVICE_NAME }}", new RouteHostMapping()).build();
            List<RouteEntry> actual = actualByClass.get(className).stream().map(SourcedRoute::entry).toList();
            assertEquals(sortedStrings(expected), sortedStrings(actual), "Route entries differ for " + className);
        }
    }

    private List<String> sortedStrings(List<RouteEntry> entries) {
        return entries.stream().map(RouteEntry::toString).sorted().toList();
    }

    private IndexView buildTestIndex() throws Exception {
        List<IndexView> indexes = new ArrayList<>();
        indexes.add(indexDirectory(TEST_CLASSES));
        for (String element : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (!element.contains("route-registration-common")) {
                continue;
            }
            File file = new File(element);
            indexes.add(file.isDirectory() ? indexDirectory(file) : indexJar(new java.util.zip.ZipFile(file)));
        }
        return CompositeIndex.create(indexes);
    }

    private IndexView indexDirectory(File directory) throws Exception {
        Indexer indexer = new Indexer();
        try (Stream<Path> files = Files.walk(directory.toPath())) {
            for (Path classFile : files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".class"))
                    .toList()) {
                try (InputStream in = Files.newInputStream(classFile)) {
                    indexer.index(in);
                }
            }
        }
        return indexer.complete();
    }

    private IndexView indexJar(java.util.zip.ZipFile zip) throws Exception {
        Indexer indexer = new Indexer();
        var entries = zip.entries();
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                try (InputStream in = zip.getInputStream(entry)) {
                    indexer.index(in);
                }
            }
        }
        return indexer.complete();
    }

    private String render(Path tempDir, String... packages) throws Exception {
        GenerateQuarkusRoutesMojo mojo = new GenerateQuarkusRoutesMojo();
        MavenProject project = testProjectWithBasedir(tempDir);
        setField(mojo, "reactorProjects", List.of(project));
        setField(mojo, "project", project);
        setField(mojo, "packages", packages);
        setField(mojo, "servicePort", 8080);
        setField(mojo, "backendRefVal", "{{ .Values.SERVICE_NAME }}");
        setField(mojo, "outputFile", "output.yaml");
        mojo.execute();
        return Files.readString(tempDir.resolve("output.yaml"));
    }

    private String renderIsolated(String... packages) throws Exception {
        Path tempDir = Files.createTempDirectory("httproutes-quarkus-test");
        try {
            return render(tempDir, packages);
        } finally {
            Files.deleteIfExists(tempDir.resolve("output.yaml"));
            Files.deleteIfExists(tempDir);
        }
    }

    private MavenProject testProject() {
        MavenProject project = new TestMavenProject();
        project.getBuild().setOutputDirectory(TEST_CLASSES.getAbsolutePath());
        return project;
    }

    private MavenProject moduleProject(Path outputDirectory) {
        MavenProject project = new TestMavenProject();
        project.getBuild().setOutputDirectory(outputDirectory.toAbsolutePath().toString());
        return project;
    }

    private static void copyPackage(Path destOutput, String pkg) throws Exception {
        Path rel = Path.of("", pkg.split("\\."));
        Path from = TEST_CLASSES.toPath().resolve(rel);
        Path to = destOutput.resolve(rel);
        try (Stream<Path> files = Files.walk(from)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Path target = to.resolve(from.relativize(file));
                Files.createDirectories(target.getParent());
                Files.copy(file, target);
            }
        }
    }

    private MavenProject testProjectWithBasedir(Path basedir) throws Exception {
        MavenProject project = testProject();
        project.setFile(basedir.resolve("pom.xml").toFile());
        return project;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Class<?> type = target.getClass();
        Field field = null;
        while (type != null) {
            try {
                field = type.getDeclaredField(name);
                break;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldException(name);
        }
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class TestMavenProject extends MavenProject {
        @Override
        public List<String> getCompileClasspathElements() {
            return Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator));
        }
    }
}
