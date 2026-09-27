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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Parameter(defaultValue = "{{ .Values.DEPLOYMENT_RESOURCE_NAME }}")
    private String backendRefVal;

    @Parameter
    private List<Label> labels = Collections.emptyList();

    /**
     * Ports of the DENY rules per border gateway name, as comma-separated lists; {@code 8080} for a gateway that isn't listed.
     */
    @Parameter
    private Map<String, String> authorizationPolicyPorts;

    /**
     * Whether to generate DENY rules for legacy implicit forbidden routes and for exposure caused by the cut.
     */
    @Parameter(defaultValue = "false")
    private boolean autoGenerateAuthorizationPolicies;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        execute(project.getBasedir().toPath(), scanner -> scanner.collectDeclarations(reactorProjects));
    }

    /**
     * Validates the configuration, scans, plans and validates the routes, and writes the output file only if
     * there is no error finding.
     */
    void execute(Path baseDir, Scan scan) throws MojoExecutionException, MojoFailureException {
        Map<Gateway, List<String>> ports = parsePorts(authorizationPolicyPorts);
        RouteDeclarations declarations = scan.declarations(new RouteScanner(packages, getLog()));
        RouteMigration migration = RouteMigration.run(declarations, autoGenerateAuthorizationPolicies);
        FindingReport.log(getLog(), migration.findings());
        if (migration.hasErrors()) {
            throw new MojoFailureException(FindingReport.summary(migration.findings()));
        }
        writeRoutesFile(baseDir, migration.plan(), ports);
    }

    @FunctionalInterface
    interface Scan {
        RouteDeclarations declarations(RouteScanner scanner) throws MojoExecutionException;
    }

    /**
     * @return the configured ports per border gateway; gateways that aren't configured are left out
     */
    static Map<Gateway, List<String>> parsePorts(Map<String, String> configured) throws MojoFailureException {
        Map<Gateway, List<String>> ports = new EnumMap<>(Gateway.class);
        if (configured == null) {
            return ports;
        }
        for (Map.Entry<String, String> entry : configured.entrySet()) {
            Gateway gateway = Gateway.fromLegacyName(entry.getKey()).orElseThrow(() -> new MojoFailureException(
                    "Unknown gateway name '" + entry.getKey() + "' in <authorizationPolicyPorts>, allowed names are "
                            + Arrays.stream(Gateway.values()).map(Gateway::legacyName).collect(Collectors.joining(", "))));
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (value.isEmpty()) {
                throw new MojoFailureException("Empty port list for '" + entry.getKey() + "' in <authorizationPolicyPorts>");
            }
            Set<String> gatewayPorts = new LinkedHashSet<>();
            for (String port : value.split(",", -1)) {
                gatewayPorts.add(parsePort(entry.getKey(), port.trim()));
            }
            ports.put(gateway, List.copyOf(gatewayPorts));
        }
        return ports;
    }

    private static String parsePort(String gatewayName, String port) throws MojoFailureException {
        int number;
        try {
            number = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            number = 0;
        }
        if (number < 1 || number > 65535) {
            throw new MojoFailureException("Invalid port '" + port + "' for '" + gatewayName
                    + "' in <authorizationPolicyPorts>, ports must be numbers from 1 to 65535");
        }
        return Integer.toString(number);
    }

    private Map<String, String> labelsAsMap() {
        if (labels == null || labels.isEmpty()) {
            return Collections.emptyMap();
        }
        return labels.stream()
                .filter(l -> l.getKey() != null)
                .collect(Collectors.toMap(Label::getKey, l -> l.getValue() != null ? l.getValue() : ""));
    }

    private void writeRoutesFile(Path baseDir, IstioPlan plan, Map<Gateway, List<String>> ports) throws MojoExecutionException {
        try {
            Path file = baseDir.resolve(outputFile);

            String yaml = new HttpRouteRenderer(backendRefVal, labelsAsMap()).generateHttpRoutesYaml(servicePort, plan.rules())
                    + new AuthorizationPolicyRenderer(labelsAsMap(), ports).generateAuthorizationPoliciesYaml(plan.denyRules());
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
