package com.netcracker.routes.gateway.plugin;

import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractGenerateRoutesMojo extends AbstractMojo {

    protected static final String MICROSERVICE_NAME_SENTINEL = "{{ .Values.SERVICE_NAME }}";

    private static final String FILE_HEADER = """
            # -----------------------------------------------------------------------------
            # THIS FILE WAS AUTOMATICALLY GENERATED — DO NOT EDIT.
            # Any changes will be overwritten during the next build.
            # Modify source annotations and regenerate using route generation maven plugin.
            # -----------------------------------------------------------------------------

            """;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    protected List<MavenProject> reactorProjects;
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;
    @Parameter(defaultValue = "com.netcracker")
    protected String[] packages;
    @Parameter(defaultValue = "8080")
    protected int servicePort;
    @Parameter(defaultValue = "gateway-httproutes.yaml")
    protected String outputFile;
    @Parameter(defaultValue = "{{ .Values.SERVICE_NAME }}")
    protected String backendRefVal;
    @Parameter
    protected List<Label> labels = Collections.emptyList();

    protected boolean sawSpringMarkers;
    protected boolean sawQuarkusMarkers;

    protected abstract List<SourcedRoute> collect(MavenProject module) throws Exception;

    protected abstract boolean isQuarkus();

    @Override
    public void execute() throws MojoExecutionException {
        try {
            sawSpringMarkers = false;
            sawQuarkusMarkers = false;
            List<SourcedRoute> allRoutes = new ArrayList<>();
            for (MavenProject module : reactorProjects) {
                allRoutes.addAll(collect(module));
            }
            assertFramework();
            Rules.checkNoPlaceholders(allRoutes);
            Map<Boolean, List<Rule>> grouped = allRoutes.stream().collect(Collectors.partitioningBy(
                    route -> route.entry().getType() == RouteType.FACADE,
                    Collectors.mapping(Rules::toRule, Collectors.toList())));
            List<Rule> gatewayRoots = RootReducer.reduce(grouped.get(false));
            List<Rule> facadeRoots = RootReducer.applyFacadeFilter(RootReducer.reduce(grouped.get(true)));
            String yaml = new HttpRouteRenderer(backendRefVal, servicePort, labelsAsMap()).render(gatewayRoots, facadeRoots);
            writeOutputFile(yaml);
        } catch (RouteGenerationException e) {
            getLog().error(e.getMessage());
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate HTTP routes: " + e.getMessage(), e);
        }
    }

    private void assertFramework() {
        if (isQuarkus() && sawSpringMarkers && !sawQuarkusMarkers) {
            throw new RouteGenerationException(FrameworkMismatch.QUARKUS_NOT_FOUND_MESSAGE);
        }
        if (!isQuarkus() && sawQuarkusMarkers && !sawSpringMarkers) {
            throw new RouteGenerationException(FrameworkMismatch.SPRING_NOT_FOUND_MESSAGE);
        }
    }

    private Map<String, String> labelsAsMap() {
        if (labels == null || labels.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        for (Label label : labels) {
            if (label.getKey() != null) {
                result.put(label.getKey(), label.getValue() != null ? label.getValue() : "");
            }
        }
        return result;
    }

    private void writeOutputFile(String yaml) throws IOException {
        Path file = project.getBasedir().toPath().resolve(outputFile);
        Files.createDirectories(file.getParent());
        Files.writeString(file, FILE_HEADER + "{{- if eq .Values.SERVICE_MESH_TYPE \"Istio\" }}\n" + yaml + "{{- end }}\n");
    }
}
