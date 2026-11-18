package com.netcracker.routes.gateway.plugin.spring;

import com.netcracker.cloud.routesregistration.common.annotation.processing.RouteHostMapping;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteEntry;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.RouteAnnotationProcessor;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.RouteFormatter;
import com.netcracker.routes.gateway.plugin.RouteGenerationException;
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
    void collectedRoutesMatchLibraryDirectly() throws Exception {
        GenerateSpringRoutesMojo mojo = new GenerateSpringRoutesMojo();
        setField(mojo, "packages", new String[]{"com.netcracker.routes.gateway.plugin.spring"});
        MavenProject module = testProject();
        List<SourcedRoute> collected = mojo.collect(module);

        for (SourcedRoute sourcedRoute : collected) {
            Class<?> clazz = Class.forName(sourcedRoute.className());
            RouteFormatter formatter = new RouteFormatter();
            formatter.setEmbeddedValueResolver(passthroughResolver());
            RouteAnnotationProcessor processor = new RouteAnnotationProcessor(formatter, null, new RouteHostMapping());
            setField(processor, "microserviceName", "{{ .Values.SERVICE_NAME }}");
            List<RouteEntry> direct = new ArrayList<>(processor.scanForRoutes(clazz));
            assertTrue(direct.contains(sourcedRoute.entry()),
                    "Collected route " + sourcedRoute.entry() + " for " + clazz.getName() + " not found in direct library scan");
        }
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
