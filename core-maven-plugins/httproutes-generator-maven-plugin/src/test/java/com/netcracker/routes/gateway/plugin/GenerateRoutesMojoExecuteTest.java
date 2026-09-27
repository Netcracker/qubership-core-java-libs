package com.netcracker.routes.gateway.plugin;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs {@link GenerateRoutesMojo#execute()} on the controller sets in the {@code e2e} test packages.
 */
class GenerateRoutesMojoExecuteTest {

    static final String E2E_PACKAGE = "com.netcracker.routes.gateway.plugin.e2e.";
    static final String OUTPUT_FILE = "target/gateway-httproutes.yaml";

    @TempDir
    Path baseDir;

    /**
     * @return a Mojo with default parameters that scans the {@code e2e.<controllerSet>} package
     */
    static GenerateRoutesMojo mojo(String controllerSet) throws Exception {
        GenerateRoutesMojo mojo = new GenerateRoutesMojo();
        set(mojo, "packages", new String[]{E2E_PACKAGE + controllerSet});
        set(mojo, "servicePort", 8080);
        set(mojo, "outputFile", OUTPUT_FILE);
        set(mojo, "backendRefVal", "{{ .Values.DEPLOYMENT_RESOURCE_NAME }}");
        return mojo;
    }

    /**
     * Runs the Mojo on the test classes directory. {@code MavenProject} can't be loaded in tests: maven-project 2.2.1
     * needs classes that the maven-artifact version on the classpath doesn't have.
     */
    static void execute(GenerateRoutesMojo mojo, Path baseDir) throws Exception {
        File classesDir = testClassesDir();
        mojo.execute(baseDir, scanner -> scanner.collectDeclarations(classesDir));
    }

    static void set(GenerateRoutesMojo mojo, String field, Object value) throws Exception {
        Field f = GenerateRoutesMojo.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(mojo, value);
    }

    private static File testClassesDir() throws Exception {
        return new File(GenerateRoutesMojoExecuteTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    static List<String> kinds(String yaml) {
        Matcher matcher = Pattern.compile("(?m)^kind: \"([^\"]+)\"$").matcher(yaml);
        return matcher.results().map(m -> m.group(1)).toList();
    }

    @Test
    void passingProjectWritesCombinedFileInOrder() throws Exception {
        execute(mojo("forbidden"), baseDir);

        String yaml = Files.readString(baseDir.resolve(OUTPUT_FILE));
        assertTrue(yaml.startsWith("# -----"), yaml);
        int guard = yaml.indexOf("{{- if eq .Values.SERVICE_MESH_TYPE \"Istio\" }}\n---\n");
        assertTrue(guard > yaml.indexOf("DO NOT EDIT"), yaml);
        assertTrue(yaml.endsWith("{{- end }}\n"), yaml);
        assertEquals(List.of("HTTPRoute", "AuthorizationPolicy", "AuthorizationPolicy", "AuthorizationPolicy"), kinds(yaml));
        assertTrue(yaml.indexOf("kind: \"HTTPRoute\"") < yaml.indexOf("kind: \"AuthorizationPolicy\""), yaml);
        assertEquals(5, yaml.split("ports:\n        - \"8080\"\n        paths:", -1).length - 1, yaml);
        assertFalse(yaml.contains("RegularExpression") || yaml.contains("MANUAL REVIEW"), yaml);
    }

    @Test
    void validationFailureLeavesExistingFileUntouched() throws Exception {
        Path file = baseDir.resolve(OUTPUT_FILE);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "previous output\n");

        // autoGenerateAuthorizationPolicies is false when it isn't configured, so the exposures are errors
        MojoFailureException e = assertThrows(MojoFailureException.class, () -> execute(mojo("unforbidden"), baseDir));

        assertTrue(e.getMessage().startsWith("Route migration validation failed with "), e.getMessage());
        assertTrue(e.getMessage().contains("EXPOSURE"), e.getMessage());
        assertEquals("previous output\n", Files.readString(file));
    }

    /**
     * Configuration errors fail the build before scanning.
     */
    private void executeWithoutScan(GenerateRoutesMojo mojo) throws Exception {
        mojo.execute(baseDir, scanner -> {
            throw new AssertionError("scanned despite a configuration error");
        });
    }

    @Test
    void unknownGatewayNameFailsTheBuild() throws Exception {
        GenerateRoutesMojo mojo = mojo("forbidden");
        set(mojo, "authorizationPolicyPorts", Map.of("facade-gateway", "8080"));

        MojoFailureException e = assertThrows(MojoFailureException.class, () -> executeWithoutScan(mojo));

        assertEquals("Unknown gateway name 'facade-gateway' in <authorizationPolicyPorts>, allowed names are "
                + "public-gateway-service, private-gateway-service, internal-gateway-service", e.getMessage());
        assertFalse(Files.exists(baseDir.resolve(OUTPUT_FILE)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "0", "65536", "abc", "8080,", "8080,abc"})
    void invalidPortsFailTheBuild(String ports) throws Exception {
        GenerateRoutesMojo mojo = mojo("forbidden");
        set(mojo, "authorizationPolicyPorts", Map.of("internal-gateway-service", ports));

        MojoFailureException e = assertThrows(MojoFailureException.class, () -> executeWithoutScan(mojo));

        assertTrue(e.getMessage().contains("'internal-gateway-service' in <authorizationPolicyPorts>"), e.getMessage());
        assertFalse(Files.exists(baseDir.resolve(OUTPUT_FILE)));
    }

    @Test
    void portsForOneGateway() throws Exception {
        GenerateRoutesMojo mojo = mojo("forbidden");
        set(mojo, "authorizationPolicyPorts", Map.of("internal-gateway-service", " 8080, 8443 "));

        execute(mojo, baseDir);

        String yaml = Files.readString(baseDir.resolve(OUTPUT_FILE));
        String internal = yaml.substring(yaml.indexOf("-java-annotations-deny-internal\""));
        assertTrue(internal.contains("ports:\n        - \"8080\"\n        - \"8443\"\n        paths:"), internal);
        String publicAndPrivate = yaml.substring(0, yaml.indexOf("-java-annotations-deny-internal\""));
        assertFalse(publicAndPrivate.contains("8443"), publicAndPrivate);
    }

    @Test
    void parsesPortsPerGateway() throws Exception {
        assertEquals(Map.of(), GenerateRoutesMojo.parsePorts(null));
        assertEquals(Map.of(Gateway.PUBLIC, List.of("8080", "9090")),
                GenerateRoutesMojo.parsePorts(Map.of("public-gateway-service", "8080, 9090,8080")));
    }
}
