package org.qubership.cloud.disableapi.quarkus;

import org.qubership.cloud.core.error.rest.tmf.TmfErrorResponse;
import org.qubership.cloud.disableapi.UrlsPatternsParser;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.spi.metadata.ResourceBuilder;
import org.jboss.resteasy.spi.metadata.ResourceClass;
import org.jboss.resteasy.spi.metadata.ResourceMethod;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@Provider
@Slf4j
public class DisableApiFilter implements ContainerRequestFilter, DynamicFeature {
    Map<AntPathMatcher, Set<String>> urlPatternsToHttpMethods;
    Map<String, Set<String>> pathsMapFromProperties = new HashMap<>();
    Map<String, Set<String>> pathsMapFromAnnotations = new HashMap<>();
    final ErrorHandler errorHandler = new ErrorHandler();
    final ResourceBuilder resourceBuilder = new ResourceBuilder();

    final boolean featureEnabled;
    @Context
    UriInfo info;

    public DisableApiFilter(
            @ConfigProperty(name = "deprecated.api.disabled") Optional<Boolean> apiDisabled,
            @ConfigProperty(name = "deprecated.api.patterns") Optional<Set<String>> patterns) {
        this.featureEnabled = apiDisabled.isPresent() && apiDisabled.get();
        if (this.featureEnabled) {
            pathsMapFromProperties.putAll(UrlsPatternsParser.parse(patterns.orElse(Collections.emptySet())));
        }
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (this.featureEnabled && (resourceInfo.getResourceMethod().getAnnotation(Deprecated.class) != null
                || resourceInfo.getResourceClass().getAnnotation(Deprecated.class) != null)) {
            ResourceClass resourceClass = resourceBuilder.getRootResourceFromAnnotations(resourceInfo.getResourceClass());
            ResourceMethod resourceMethod = Arrays.stream(resourceClass.getResourceMethods())
                    .filter(rMethod -> Objects.equals(rMethod.getMethod(), resourceInfo.getResourceMethod()))
                    .findFirst().orElseThrow(() -> new IllegalStateException("Failed to find resource method: " + resourceInfo.getResourceMethod()));
            String fullPath = resourceMethod.getFullpath();
            Set<String> httpMethods = resourceMethod.getHttpMethods();
            pathsMapFromAnnotations.computeIfAbsent(fullPath, m -> new HashSet<>()).addAll(httpMethods);
        }
    }

    public void onStartup(@Observes StartupEvent startupEvent) {
        if (this.featureEnabled) {
            Map<String, Set<String>> pathsMap;
            if (!pathsMapFromProperties.isEmpty() && !pathsMapFromAnnotations.isEmpty()) {
                throw new IllegalStateException("Found deprecated REST endpoints both from 'deprecated.api.patterns' property and " +
                        "from the code annotated with @Deprecated annotation! Cannot use both approaches simultaneously.");
            } else if (!pathsMapFromProperties.isEmpty()) {
                log.info("Using paths from 'deprecated.api.patterns' property");
                pathsMap = new HashMap<>(pathsMapFromProperties);
                pathsMapFromProperties = null;
            } else {
                log.info("Using paths annotated by @Deprecated annotation");
                pathsMap = new HashMap<>(pathsMapFromAnnotations);
                pathsMapFromAnnotations = null;
            }
            log.warn("Disabling the following deprecated paths: \n{}",
                    pathsMap.entrySet().stream()
                            .map(entry -> String.format("%s %s", entry.getKey(),
                                    entry.getValue().stream().sorted().collect(Collectors.toList())))
                            .sorted()
                            .collect(Collectors.joining("\n")));
            urlPatternsToHttpMethods = pathsMap.entrySet().stream().collect(Collectors.toMap(
                    entry -> new AntPathMatcher(entry.getKey()),
                    Map.Entry::getValue));
        } else {
            urlPatternsToHttpMethods = Collections.emptyMap();
        }
    }

    @Override
    public void filter(ContainerRequestContext rc) throws IOException {
        if (this.featureEnabled) {
            final String method = rc.getMethod().toUpperCase();
            final String path = info.getPath();
            Map.Entry<AntPathMatcher, Set<String>> deprecatedPathMethods = urlPatternsToHttpMethods.entrySet().stream()
                    .filter(entry -> entry.getKey().matches(path) &&
                            (entry.getValue().contains(UrlsPatternsParser.WILDCARD) || entry.getValue().contains(method)))
                    .findFirst().orElse(null);
            if (deprecatedPathMethods != null) {
                TmfErrorResponse tmfErrorResponse = errorHandler.buildErrorResponse(method, path,
                        deprecatedPathMethods.getKey().getAntPath(), deprecatedPathMethods.getValue());
                rc.abortWith(Response.status(Response.Status.NOT_FOUND)
                        .entity(tmfErrorResponse)
                        .type(MediaType.APPLICATION_JSON).build());
            }
        }
    }
}