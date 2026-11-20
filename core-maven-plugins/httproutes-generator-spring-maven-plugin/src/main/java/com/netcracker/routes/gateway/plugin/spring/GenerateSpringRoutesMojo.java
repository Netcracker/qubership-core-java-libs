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
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

@Mojo(
        name = "generate-routes",
        aggregator = true,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        threadSafe = true
)
public class GenerateSpringRoutesMojo extends AbstractGenerateRoutesMojo {

    private static final String MICROSERVICE_NAME_SENTINEL = "{{ .Values.SERVICE_NAME }}";

    @Override
    protected void assertFramework() {
        if (sawQuarkusMarkers && !sawSpringMarkers) {
            throw new RouteGenerationException(FrameworkMismatch.SPRING_NOT_FOUND_MESSAGE);
        }
    }

    @Override
    protected List<SourcedRoute> collect(MavenProject module) throws Exception {
        File outputDirectory = new File(module.getBuild().getOutputDirectory());
        List<String> classNames = Rules.classNames(outputDirectory, packages);
        if (classNames.isEmpty()) {
            return List.of();
        }

        try (URLClassLoader loader = buildClassLoader(module, outputDirectory)) {
            List<SourcedRoute> routes = new ArrayList<>();
            for (String className : classNames) {
                Class<?> candidate = loadClass(className, loader);
                recordFrameworkMarkers(candidate);
                if (!isCandidate(candidate)) {
                    continue;
                }
                routes.addAll(collectFromClass(candidate));
            }
            return routes;
        }
    }

    private void recordFrameworkMarkers(Class<?> clazz) {
        if (AnnotatedElementUtils.hasAnnotation(clazz, RestController.class)
                || AnnotatedElementUtils.hasAnnotation(clazz, Controller.class)) {
            sawSpringMarkers = true;
        }
        if (hasJaxRsPath(clazz)) {
            sawQuarkusMarkers = true;
        }
    }

    private boolean hasJaxRsPath(Class<?> clazz) {
        if (isJaxRsPathAnnotated(clazz)) {
            return true;
        }
        for (Method method : clazz.getDeclaredMethods()) {
            if (isJaxRsPathAnnotated(method)) {
                return true;
            }
        }
        return false;
    }

    private boolean isJaxRsPathAnnotated(AnnotatedElement element) {
        for (Annotation annotation : element.getAnnotations()) {
            String name = annotation.annotationType().getName();
            if (FrameworkMismatch.JAKARTA_WS_RS_PATH.equals(name)
                    || FrameworkMismatch.JAVAX_WS_RS_PATH.equals(name)) {
                return true;
            }
        }
        return false;
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
        if (candidate.isInterface() || Modifier.isAbstract(candidate.getModifiers())) {
            return false;
        }
        if (!AnnotatedElementUtils.isAnnotated(candidate, Component.class)) {
            return false;
        }
        return RouteAnnotationProcessorUtil.isAnnotatedWithRoute(candidate)
                || RouteAnnotationProcessorUtil.isAnnotatedWithFacadeRoute(candidate);
    }

    private List<SourcedRoute> collectFromClass(Class<?> candidate) {
        RouteFormatter formatter = new RouteFormatter();
        formatter.setEmbeddedValueResolver(new ThrowingStringValueResolver());

        RouteAnnotationProcessor processor = new RouteAnnotationProcessor(formatter, null, new RouteHostMapping());
        setMicroserviceName(processor, MICROSERVICE_NAME_SENTINEL);

        try {
            List<SourcedRoute> result = new ArrayList<>();
            for (RouteEntry entry : processor.scanForRoutes(candidate)) {
                result.add(new SourcedRoute(candidate.getName(), entry));
            }
            return result;
        } catch (Exception e) {
            throw new RouteGenerationException("Class " + candidate.getName() + ": " + e.getMessage());
        }
    }

    private void setMicroserviceName(RouteAnnotationProcessor processor, String value) {
        try {
            Field field = RouteAnnotationProcessor.class.getDeclaredField("microserviceName");
            field.setAccessible(true);
            field.set(processor, value);
        } catch (ReflectiveOperationException e) {
            throw new RouteGenerationException("Failed to set microserviceName sentinel: " + e.getMessage());
        }
    }

    private static class ThrowingStringValueResolver implements StringValueResolver {
        @Override
        public String resolveStringValue(String placeholder) {
            throw new RouteGenerationException("Unresolvable placeholder " + placeholder);
        }
    }
}