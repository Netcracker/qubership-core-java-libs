package com.netcracker.routes.gateway.plugin;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
        mojo.execute(baseDir, scanner -> scanner.collect(classesDir));
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

        assertEquals("2 route migration errors, see log", e.getMessage());
        assertEquals("previous output\n", Files.readString(file));
    }
}
