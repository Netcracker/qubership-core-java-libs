package com.netcracker.cloud.plugins.pins;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckMojoTest {

    @Test
    void noExcludesFileMeansNothingIsExcluded() throws Exception {
        assertEquals(List.of(), CheckMojo.readExcludes(null));
    }

    @Test
    void excludesIgnoreBlankLinesAndMatchAnyPartOfThePath(@TempDir Path dir) throws Exception {
        Path file = Files.writeString(dir.resolve("excludes.txt"), """
                generated/

                /broken/
                """);
        List<Pattern> excludes = CheckMojo.readExcludes(file.toFile());
        assertEquals(2, excludes.size());
        assertTrue(CheckMojo.excluded("app/generated/pom.xml", excludes));
        assertTrue(CheckMojo.excluded("modules/broken/pom.xml", excludes));
        assertFalse(CheckMojo.excluded("app/lib/pom.xml", excludes));
    }

    @Test
    void invalidExcludeRegexFailsClosed(@TempDir Path dir) throws Exception {
        Path file = Files.writeString(dir.resolve("excludes.txt"), "(");
        MojoExecutionException e = assertThrows(MojoExecutionException.class,
                () -> CheckMojo.readExcludes(file.toFile()));
        assertTrue(e.getMessage().startsWith("Cannot read the excludes from "), e.getMessage());
        assertTrue(e.getMessage().contains("Unclosed group"), e.getMessage());
    }

    @Test
    void unbuildableRootsFailClosed() throws Exception {
        MojoExecutionException e = assertThrows(MojoExecutionException.class,
                () -> CheckMojo.requireBuilt(List.of("broken/pom.xml (Parent relative path)")));
        assertEquals("Cannot build broken/pom.xml (Parent relative path)", e.getMessage());
        CheckMojo.requireBuilt(List.of());
    }

    @Test
    void excludedModulesAreNotEvaluated() throws Exception {
        var projects = Fixtures.projects(Fixtures.REPO, new Properties());
        var collector = new UsageCollector(Fixtures.REPO, new Properties()).collect(projects, projects);
        List<Pattern> excludes = List.of(Pattern.compile("quarkus-app/"));
        var checked = projects.stream()
                .filter(p -> !CheckMojo.excluded(collector.relative(p.getFile().toPath()), excludes)).toList();
        assertTrue(checked.stream().noneMatch(p -> p.getArtifactId().equals("fixture-quarkus-app")));
        var findings = Fixtures.byKey(Fixtures.check(
                new UsageCollector(Fixtures.REPO, new Properties()).collect(projects, checked)));
        assertFalse(findings.containsKey("pin-exceeded|io.quarkus:quarkus-core|3.40.1"), findings.keySet().toString());
        assertTrue(findings.containsKey("pin-exceeded|com.rabbitmq:amqp-client|5.37.0"), findings.keySet().toString());
    }

    /**
     * The goal as Maven runs it: the generated descriptor of {@code check}, the pom's plugin configuration merged over
     * the descriptor defaults, the pom's properties and {@code -D}. No {@code expected} means the check runs.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            nothing in the pom                      |                                           |                                     | dependency-gate.pins=pins.json      |
            -D only, several parameters             |                                           |                                     | dependency-gate.pins=p.json;dependency-gate.transitive=true;dependency-gate.findings=f.json |
            configuration over -D                   | <pins>lax.json</pins>                     |                                     | dependency-gate.pins=pins.json      | dependency-gate.pins (configured as lax.json)
            configuration not given with -D         | <excludesFile>excludes.txt</excludesFile> |                                     |                                     | dependency-gate.excludesFile (configured as excludes.txt)
            configured list                         | <poms><pom>a</pom><pom>b</pom></poms>     |                                     |                                     | dependency-gate.poms (configured as [a,b])
            configured as its own expression        | <pins>${dependency-gate.pins}</pins>      |                                     | dependency-gate.pins=pins.json      |
            several at once                         | <skip>true</skip><revision>base</revision> |                                    |                                     | dependency-gate.skip (configured as true);dependency-gate.revision (configured as base)
            pom property not given with -D          |                                           | dependency-gate.root=elsewhere      |                                     | dependency-gate.root (set to elsewhere not with -D)
            pom property differs from -D            |                                           | dependency-gate.transitive=false    | dependency-gate.transitive=true     |
            maxLocations may be configured          | <maxLocations>1</maxLocations>            | dependency-gate.maxLocations=2      |                                     |""")
    void gateParametersComeOnlyFromTheCommandLine(String name, String configuration, String pomProperties,
                                                  String userProperties, String expected) throws Exception {
        CheckMojo mojo = new CheckMojo(null, null);
        mojo.session = session(properties(pomProperties), properties(userProperties));
        mojo.mojoExecution = execution(Objects.toString(configuration, ""));
        if (expected == null) {
            mojo.checkParameters();
            return;
        }
        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::checkParameters);
        assertTrue(e.getMessage().startsWith("Parameters of the dependency gate are set in the pom: "), e.getMessage());
        List<String> parts = List.of(expected.split(";"));
        parts.forEach(part -> assertTrue(e.getMessage().contains(part), e.getMessage()));
        assertEquals(parts.size(), e.getMessage().split(" \\((configured as|set to) ").length - 1, e.getMessage());
    }

    private static MojoExecution execution(String configuration) throws Exception {
        PluginDescriptor plugin;
        try (Reader descriptor = new InputStreamReader(
                CheckMojoTest.class.getResourceAsStream("/META-INF/maven/plugin.xml"), StandardCharsets.UTF_8)) {
            plugin = new PluginDescriptorBuilder().build(descriptor);
        }
        MojoDescriptor check = plugin.getMojo("check");
        // what Maven runs: the configuration of the pom, completed with the descriptor's expressions and defaults
        Xpp3Dom pom = Xpp3DomBuilder.build(new StringReader("<configuration>" + configuration + "</configuration>"));
        return new MojoExecution(check, Xpp3Dom.mergeXpp3Dom(pom, MojoDescriptorCreator.convert(check)));
    }

    private static MavenSession session(Properties pomProperties, Properties userProperties) {
        Model model = new Model();
        model.setProperties(pomProperties);
        DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setUserProperties(userProperties);
        @SuppressWarnings("deprecation")
        MavenSession session = new MavenSession(null, null, request, new DefaultMavenExecutionResult());
        session.setCurrentProject(new MavenProject(model));
        return session;
    }

    private static Properties properties(String pairs) {
        Properties properties = new Properties();
        for (String pair : Objects.toString(pairs, "").split(";")) {
            if (!pair.isBlank()) {
                String[] kv = pair.split("=", 2);
                properties.setProperty(kv[0].trim(), kv[1].trim());
            }
        }
        return properties;
    }
}
