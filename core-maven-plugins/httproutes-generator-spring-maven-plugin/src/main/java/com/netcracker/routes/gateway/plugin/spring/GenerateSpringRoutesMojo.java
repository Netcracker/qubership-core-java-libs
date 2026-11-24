package com.netcracker.routes.gateway.plugin.spring;

import com.netcracker.cloud.routesregistration.common.annotation.processing.RouteHostMapping;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteEntry;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.RouteAnnotationProcessor;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.RouteAnnotationProcessorUtil;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.RouteFormatter;
import com.netcracker.routes.gateway.plugin.AbstractGenerateRoutesMojo;
import com.netcracker.routes.gateway.plugin.FrameworkMismatch;
import com.netcracker.routes.gateway.plugin.RouteGenerationException;
import com.netcracker.routes.gateway.plugin.Rules;
import com.netcracker.routes.gateway.plugin.SourcedRoute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mojo(
        name = "generate-routes",
        aggregator = true,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        threadSafe = true
)
public class GenerateSpringRoutesMojo extends AbstractGenerateRoutesMojo {

    @Override
    protected boolean isQuarkus() {
        return false;
    }

    @Override
    protected List<SourcedRoute> collect(MavenProject module) throws Exception {
        File outputDirectory = new File(module.getBuild().getOutputDirectory());
        List<String> classNames = Rules.classNames(outputDirectory, packages);
        if (classNames.isEmpty()) {
            return List.of();
        }

        RouteAnnotationProcessor processor = null;
        try (URLClassLoader loader = buildClassLoader(module, outputDirectory)) {
            List<SourcedRoute> routes = new ArrayList<>();
            for (String className : classNames) {
                Class<?> candidate = loadClass(className, loader);
                recordFrameworkMarkers(candidate);
                if (isCandidate(candidate)) {
                    if (processor == null) {
                        processor = createProcessor();
                    }
                    collectFromClass(processor, candidate, routes);
                }
            }
            return routes;
        }
    }

    private void recordFrameworkMarkers(Class<?> clazz) {
        if (AnnotatedElementUtils.hasAnnotation(clazz, RestController.class)
                || AnnotatedElementUtils.hasAnnotation(clazz, Controller.class)) {
            sawSpringMarkers = true;
        }
        if (hasJaxRsPath(clazz) || Arrays.stream(clazz.getDeclaredMethods()).anyMatch(this::hasJaxRsPath)) {
            sawQuarkusMarkers = true;
        }
    }

    private boolean hasJaxRsPath(AnnotatedElement element) {
        return Arrays.stream(element.getAnnotations())
                .map(a -> a.annotationType().getName())
                .anyMatch(name -> FrameworkMismatch.JAKARTA_WS_RS_PATH.equals(name) || FrameworkMismatch.JAVAX_WS_RS_PATH.equals(name));
    }

    private URLClassLoader buildClassLoader(MavenProject module, File outputDirectory) throws Exception {
        List<URL> urls = new ArrayList<>();
        urls.add(outputDirectory.toURI().toURL());
        for (Object element : module.getCompileClasspathElements()) {
            urls.add(new File((String) element).toURI().toURL());
        }
        return new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
    }

    private Class<?> loadClass(String className, ClassLoader loader) {
        try {
            return Class.forName(className, false, loader);
        } catch (Throwable e) {
            throw new RouteGenerationException("Failed to load class " + className + ": " + e.getMessage());
        }
    }

    private boolean isCandidate(Class<?> candidate) {
        return !candidate.isInterface()
                && !Modifier.isAbstract(candidate.getModifiers())
                && AnnotatedElementUtils.isAnnotated(candidate, Component.class)
                && (RouteAnnotationProcessorUtil.isAnnotatedWithRoute(candidate)
                        || RouteAnnotationProcessorUtil.isAnnotatedWithFacadeRoute(candidate));
    }

    private RouteAnnotationProcessor createProcessor() {
        RouteFormatter formatter = new RouteFormatter();
        formatter.setEmbeddedValueResolver(placeholder -> {
            throw new RouteGenerationException("Unresolvable placeholder " + placeholder);
        });
        RouteAnnotationProcessor processor = new RouteAnnotationProcessor(formatter, null, new RouteHostMapping());
        try {
            Field field = RouteAnnotationProcessor.class.getDeclaredField("microserviceName");
            field.setAccessible(true);
            field.set(processor, MICROSERVICE_NAME_SENTINEL);
        } catch (ReflectiveOperationException e) {
            throw new RouteGenerationException("Failed to set microserviceName sentinel: " + e.getMessage());
        }
        return processor;
    }

    private void collectFromClass(RouteAnnotationProcessor processor, Class<?> candidate, List<SourcedRoute> routes) {
        try {
            for (RouteEntry entry : processor.scanForRoutes(candidate)) {
                routes.add(new SourcedRoute(candidate.getName(), entry));
            }
        } catch (Exception e) {
            throw new RouteGenerationException("Class " + candidate.getName() + ": " + e.getMessage());
        }
    }
}
