package com.netcracker.routes.gateway.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mojo(
        name = "generate-routes",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        aggregator = true
)
public class GenerateRoutesMojo extends AbstractMojo {

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "com.netcracker")
    private String[] packages;

    @Parameter(defaultValue = "8080")
    private int servicePort;

    @Parameter(defaultValue = "gateway-httproutes.yaml")
    private String outputFile;

    @Parameter(defaultValue = "{{ .Values.SERVICE_NAME }}")
    private String backendRefVal;

    @Parameter
    private List<Label> labels = Collections.emptyList();

    /**
     * Whether to generate the DENY rules that the migration needs and {@code @ForbiddenRoute} doesn't declare.
     */
    @Parameter(defaultValue = "false")
    private boolean autoGenerateAuthorizationPolicies;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        execute(project.getBasedir().toPath(), scanner -> scanner.collect(reactorProjects));
    }

    /**
     * Scans and renders the routes, and writes the output file only if there are no errors.
     */
    void execute(Path baseDir, Scan scan) throws MojoExecutionException, MojoFailureException {
        RouteScanner.Declarations declarations = scan.declarations(new RouteScanner(packages, getLog()));
        Problems problems = new Problems();
        problems.errors().addAll(declarations.errors());
        String yaml = new HttpRouteRenderer(backendRefVal, labelsAsMap())
                .generateHttpRoutesYaml(servicePort, declarations.routes(), problems)
                + new AuthorizationPolicyRenderer(labelsAsMap(), autoGenerateAuthorizationPolicies)
                .generateAuthorizationPoliciesYaml(declarations.routes(), declarations.forbidden(), problems);
        problems.warnings().forEach(getLog()::warn);
        problems.errors().forEach(getLog()::error);
        if (!problems.errors().isEmpty()) {
            throw new MojoFailureException(problems.errors().size() + " route migration errors, see log");
        }
        writeRoutesFile(baseDir, yaml);
    }

    @FunctionalInterface
    interface Scan {
        RouteScanner.Declarations declarations(RouteScanner scanner) throws MojoExecutionException;
    }

    private Map<String, String> labelsAsMap() {
        if (labels == null || labels.isEmpty()) {
            return Collections.emptyMap();
        }
        return labels.stream()
                .filter(l -> l.getKey() != null)
                .collect(Collectors.toMap(Label::getKey, l -> l.getValue() != null ? l.getValue() : ""));
    }

    private void writeRoutesFile(Path baseDir, String yaml) throws MojoExecutionException {
        try {
            Path file = baseDir.resolve(outputFile);

            Files.createDirectories(file.getParent());
            Files.writeString(file, prependYamlHeader(wrapWithEnabler(yaml)));
            getLog().info(String.format("Generated gateway routes CRs at %s", outputFile));

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate routes", e);
        }
    }

    private String prependYamlHeader(String yamlContent) {
        return """
                # -----------------------------------------------------------------------------
                # THIS FILE WAS AUTOMATICALLY GENERATED — DO NOT EDIT.
                # Any changes will be overwritten during the next build.
                # Modify source annotations and regenerate using route generation maven plugin.
                # -----------------------------------------------------------------------------

                """ + yamlContent;
    }

    private String wrapWithEnabler(String yamlContent) {
        return "{{- if eq .Values.SERVICE_MESH_TYPE \"Istio\" }}\n" + yamlContent + "{{- end }}\n";
    }

    public static class Label {

        private String key;
        private String value;

        public Label() {
        }

        public Label(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
