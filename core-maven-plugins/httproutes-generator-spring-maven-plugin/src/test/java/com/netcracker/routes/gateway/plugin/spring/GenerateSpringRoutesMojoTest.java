package com.netcracker.routes.gateway.plugin.spring;

import com.netcracker.cloud.routesregistration.common.annotation.processing.RouteHostMapping;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteEntry;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.RouteAnnotationProcessor;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.RouteFormatter;
import com.netcracker.routes.gateway.plugin.RouteGenerationException;
import com.netcracker.routes.gateway.plugin.FrameworkMismatch;
import com.netcracker.routes.gateway.plugin.SourcedRoute;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.StringValueResolver;

import java.io.File;
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

class GenerateSpringRoutesMojoTest {

    private static final File TEST_CLASSES = new File("target/test-classes").getAbsoluteFile();

    @Test
    void mainFixturesProduceExpectedReferenceRoots(@TempDir Path tempDir) throws Exception {
        String yaml = render(tempDir, "com.netcracker.routes.gateway.plugin.spring");

        // Spring1 -> /api/v1/test1, /api/v1/test2 at 250s
        assertTrue(yaml.contains("value: /api/v1/test1"));
        assertTrue(yaml.contains("value: /api/v1/test2"));
        assertTrue(yaml.contains("request: 250s"));

        // Spring2 -> /test1, /test2
        assertTrue(yaml.contains("value: /test1"));
        assertTrue(yaml.contains("value: /test2"));

        // Spring3 -> /class/path/from(,2) -> /class/path/to, plus nested method roots
        assertTrue(yaml.contains("value: /class/path/from"));
        assertTrue(yaml.contains("value: /class/path/from2"));
        assertTrue(yaml.contains("replacePrefixMatch: /class/path/to"));
        assertTrue(yaml.contains("value: /class/path/from/method/path/from"));
        assertTrue(yaml.contains("value: /class/path/from/method/path/from2"));
        assertTrue(yaml.contains("value: /class/path/from2/method/path/from"));
        assertTrue(yaml.contains("value: /class/path/from2/method/path/from2"));

        // Spring6 base -> /custom/test1 -> /api/v1/test4/test1
        assertTrue(yaml.contains("value: /custom/test1"));
        assertTrue(yaml.contains("replacePrefixMatch: /api/v1/test4/test1"));
    }

    @Test
    void concreteSubclassInheritsClassAndMethodAnnotations(@TempDir Path tempDir) throws Exception {
        String yaml = render(tempDir, "com.netcracker.routes.gateway.plugin.spring");
        assertTrue(yaml.contains("value: /api/v1/base10/items"));
    }

    @Test
    void subclassOverridesClassLevelPath(@TempDir Path tempDir) throws Exception {
        String yaml = render(tempDir, "com.netcracker.routes.gateway.plugin.spring");
        assertTrue(yaml.contains("value: /old11"));
        assertTrue(yaml.contains("value: /new11"));
    }

    @Test
    void gatewayAnnotationIgnoredForSpring(@TempDir Path tempDir) throws Exception {
        String yaml = render(tempDir, "com.netcracker.routes.gateway.plugin.spring");
        assertTrue(yaml.contains("value: /svc12"));
        assertFalse(yaml.contains("/gw12"));
    }

    @Test
    void repeatedRouteYieldsGatewayAndFacadeRoutes() throws Exception {
        List<SourcedRoute> routes = collect("com.netcracker.routes.gateway.plugin.spring");
        List<RouteType> types = routesFor(routes, "/svc13/items");
        assertTrue(types.contains(RouteType.PUBLIC));
        assertTrue(types.contains(RouteType.FACADE));
    }

    @Test
    void borderGatewayNameIsGatewayGroupAndCompositeIsFacade() throws Exception {
        List<SourcedRoute> routes = collect("com.netcracker.routes.gateway.plugin.spring");
        assertFalse(routesFor(routes, "/svc14/border").contains(RouteType.FACADE));
        assertTrue(routesFor(routes, "/svc14/composite").contains(RouteType.FACADE));
    }

    @Test
    void hostsAreIgnoredInRenderedOutput(@TempDir Path tempDir) throws Exception {
        String yaml = render(tempDir, "com.netcracker.routes.gateway.plugin.spring");
        assertFalse(yaml.contains("example.com"));
        assertFalse(yaml.contains("hostnames"));
    }

    @Test
    void spring8ScannedAloneHasNoFacadeHttpRoute() throws Exception {
        String yaml = renderWithoutTempFile("com.netcracker.routes.gateway.plugin.springfacade8");
        assertTrue(yaml.contains("value: /class/path/from"));
        assertTrue(yaml.contains("replacePrefixMatch: /class/path/to"));
        assertTrue(yaml.contains("request: 20s"));
        assertFalse(yaml.contains("java-annotations-facade"));
    }

    @Test
    void spring8CombinedWithFacadeGatewayMappingRendersBothFacadeRoots() throws Exception {
        String yaml = renderWithoutTempFile(
                "com.netcracker.routes.gateway.plugin.springfacade8",
                "com.netcracker.routes.gateway.plugin.springfacade8combined");
        assertTrue(yaml.contains("java-annotations-facade"));
        assertTrue(yaml.contains("value: /facade"));
        assertTrue(yaml.contains("replacePrefixMatch: /svc"));
        assertTrue(yaml.contains("request: 10s"));
    }

    @Test
    void gatewayMappingDoesNotApplyToFacade() throws Exception {
        String yaml = renderWithoutTempFile("com.netcracker.routes.gateway.plugin.springgatewaymapping");
        assertFalse(yaml.contains("java-annotations-facade"));
        assertFalse(yaml.contains("/gw17"));
    }

    @Test
    void quarkusGuardFailsWithExplicitMessage() {
        MojoExecutionException ex = assertThrows(MojoExecutionException.class,
                () -> renderWithoutTempFile("com.netcracker.routes.gateway.plugin.jaxrsguard"));
        assertEquals(FrameworkMismatch.SPRING_NOT_FOUND_MESSAGE, ex.getMessage());
    }

    @Test
    void springWebMixedDoesNotFailFrameworkMismatch(@TempDir Path tempDir) throws Exception {
        render(tempDir, "com.netcracker.routes.gateway.plugin.springwebmixed");
    }

    @Test
    void reactorWideJaxRsApiAndSpringServiceDoesNotFail(@TempDir Path tempDir) throws Exception {
        Path apiOut = tempDir.resolve("api-classes");
        Path serviceOut = tempDir.resolve("service-classes");
        copyPackage(apiOut, "com.netcracker.routes.gateway.plugin.jaxrsguard");
        copyPackage(serviceOut, "com.netcracker.routes.gateway.plugin.spring");

        GenerateSpringRoutesMojo mojo = new GenerateSpringRoutesMojo();
        MavenProject executing = testProjectWithBasedir(tempDir);
        setField(mojo, "reactorProjects", List.of(moduleProject(apiOut), moduleProject(serviceOut)));
        setField(mojo, "project", executing);
        setField(mojo, "packages", new String[]{
                "com.netcracker.routes.gateway.plugin.jaxrsguard",
                "com.netcracker.routes.gateway.plugin.spring"
        });
        setField(mojo, "servicePort", 8080);
        setField(mojo, "backendRefVal", "{{ .Values.SERVICE_NAME }}");
        setField(mojo, "outputFile", "output.yaml");
        mojo.execute();

        String yaml = Files.readString(tempDir.resolve("output.yaml"));
        assertTrue(yaml.contains("value: /api/v1/test1"));
    }

    @Test
    void springPlaceholderInClassPathFailsNamingClassAndPlaceholder() {
        MojoExecutionException ex = assertThrows(MojoExecutionException.class,
                () -> renderWithoutTempFile("com.netcracker.routes.gateway.plugin.springplaceholder"));
        assertTrue(ex.getMessage().contains("ControllerWithPlaceholder"));
        assertTrue(ex.getMessage().contains("${api.prefix}"));
    }

    @Test
    void rootSlashFailsNamingTheClass() {
        MojoExecutionException ex = assertThrows(MojoExecutionException.class,
                () -> renderWithoutTempFile("com.netcracker.routes.gateway.plugin.springrootslash"));
        assertTrue(ex.getMessage().contains("ControllerWithNoClassPath"));
    }

    @Test
    void writesCombinedOutputForIntegrationCheck() throws Exception {
        Path targetDir = new File("target/httproutes-test").getAbsoluteFile().toPath();
        Files.createDirectories(targetDir);
        GenerateSpringRoutesMojo mojo = new GenerateSpringRoutesMojo();
        MavenProject project = new TestMavenProject();
        project.getBuild().setOutputDirectory(TEST_CLASSES.getAbsolutePath());
        project.setFile(targetDir.resolve("pom.xml").toFile());
        setField(mojo, "reactorProjects", List.of(project));
        setField(mojo, "project", project);
        setField(mojo, "packages", new String[]{
                "com.netcracker.routes.gateway.plugin.spring",
                "com.netcracker.routes.gateway.plugin.springfacade8",
                "com.netcracker.routes.gateway.plugin.springfacade8combined"
        });
        setField(mojo, "servicePort", 8080);
        setField(mojo, "backendRefVal", "{{ .Values.SERVICE_NAME }}");
        setField(mojo, "outputFile", "annotations-httproutes.yaml");
        mojo.execute();

        String yaml = Files.readString(targetDir.resolve("annotations-httproutes.yaml"));
        assertTrue(yaml.contains("java-annotations\""));
        assertTrue(yaml.contains("java-annotations-facade"));
    }

    @Test
    void collectedRoutesMatchLibraryExactly() throws Exception {
        String pkg = "com.netcracker.routes.gateway.plugin.spring";
        GenerateSpringRoutesMojo mojo = new GenerateSpringRoutesMojo();
        setField(mojo, "packages", new String[]{pkg});
        List<SourcedRoute> collected = mojo.collect(testProject());

        Set<String> expectedCandidates = independentlyFindCandidates(pkg);
        Set<String> actualCandidates = collected.stream().map(SourcedRoute::className).collect(Collectors.toSet());
        assertEquals(expectedCandidates, actualCandidates, "Candidate class set differs from an independent scan");

        Map<String, List<SourcedRoute>> actualByClass = collected.stream().collect(Collectors.groupingBy(SourcedRoute::className));
        for (String className : expectedCandidates) {
            Class<?> clazz = Class.forName(className);
            List<RouteEntry> expected = new ArrayList<>(scanDirectly(clazz));
            List<RouteEntry> actual = actualByClass.get(className).stream().map(SourcedRoute::entry).toList();
            assertEquals(sortedStrings(expected), sortedStrings(actual), "Route entries differ for " + className);
        }
    }

    private Set<String> independentlyFindCandidates(String pkg) throws Exception {
        Set<String> candidates = new java.util.HashSet<>();
        for (String className : com.netcracker.routes.gateway.plugin.Rules.classNames(TEST_CLASSES, new String[]{pkg})) {
            Class<?> clazz = Class.forName(className);
            if (clazz.isInterface() || java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }
            if (!org.springframework.core.annotation.AnnotatedElementUtils.isAnnotated(clazz, org.springframework.stereotype.Component.class)) {
                continue;
            }
            if (com.netcracker.cloud.routesregistration.common.spring.gateway.route.RouteAnnotationProcessorUtil.isAnnotatedWithRoute(clazz)
                    || com.netcracker.cloud.routesregistration.common.spring.gateway.route.RouteAnnotationProcessorUtil.isAnnotatedWithFacadeRoute(clazz)) {
                candidates.add(className);
            }
        }
        return candidates;
    }

    private List<RouteEntry> scanDirectly(Class<?> clazz) throws Exception {
        RouteFormatter formatter = new RouteFormatter();
        formatter.setEmbeddedValueResolver(passthroughResolver());
        RouteAnnotationProcessor processor = new RouteAnnotationProcessor(formatter, null, new RouteHostMapping());
        setField(processor, "microserviceName", "{{ .Values.SERVICE_NAME }}");
        return new ArrayList<>(processor.scanForRoutes(clazz));
    }

    private List<String> sortedStrings(List<RouteEntry> entries) {
        return entries.stream().map(RouteEntry::toString).sorted().toList();
    }

    private List<SourcedRoute> collect(String... packages) throws Exception {
        GenerateSpringRoutesMojo mojo = new GenerateSpringRoutesMojo();
        setField(mojo, "packages", packages);
        return mojo.collect(testProject());
    }

    private List<RouteType> routesFor(List<SourcedRoute> routes, String fromPath) {
        return routes.stream()
                .filter(r -> fromPath.equals(r.entry().getFrom()))
                .map(r -> r.entry().getType())
                .toList();
    }

    private String render(Path tempDir, String... packages) throws Exception {
        GenerateSpringRoutesMojo mojo = new GenerateSpringRoutesMojo();
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

    private String renderWithoutTempFile(String... packages) throws Exception {
        Path tempDir = Files.createTempDirectory("httproutes-test");
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

    private static class TestMavenProject extends MavenProject {
        @Override
        public List<String> getCompileClasspathElements() {
            return Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator));
        }
    }

    private MavenProject testProjectWithBasedir(Path basedir) throws Exception {
        MavenProject project = testProject();
        project.setFile(basedir.resolve("pom.xml").toFile());
        return project;
    }

    private StringValueResolver passthroughResolver() {
        return strVal -> strVal;
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
}
