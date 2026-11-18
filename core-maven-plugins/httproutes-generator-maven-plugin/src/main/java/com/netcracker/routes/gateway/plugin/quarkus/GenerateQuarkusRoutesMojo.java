package com.netcracker.routes.gateway.plugin.quarkus;

import com.netcracker.cloud.quarkus.routesregistration.deployment.RoutesRegistrationProcessor;
import com.netcracker.cloud.routesregistration.common.annotation.processing.RouteHostMapping;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteEntry;
import com.netcracker.routes.gateway.plugin.AbstractGenerateRoutesMojo;
import com.netcracker.routes.gateway.plugin.RouteGenerationException;
import com.netcracker.routes.gateway.plugin.Rules;
import com.netcracker.routes.gateway.plugin.SourcedRoute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Mojo(
        name = "generate-routes",
        aggregator = true,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        threadSafe = true
)
public class GenerateQuarkusRoutesMojo extends AbstractGenerateRoutesMojo {

    private static final String MICROSERVICE_NAME_SENTINEL = "{{ .Values.SERVICE_NAME }}";
    private static final String SPRING_REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController";
    private static final String SPRING_CONTROLLER = "org.springframework.stereotype.Controller";

    @Override
    protected List<SourcedRoute> collect(MavenProject module) throws Exception {
        File outputDirectory = new File(module.getBuild().getOutputDirectory());
        List<String> classNames = Rules.classNames(outputDirectory, packages);
        if (classNames.isEmpty()) {
            return List.of();
        }

        IndexView index = buildIndex(module, outputDirectory);
        checkNotSpring(index, classNames);

        RoutesRegistrationProcessor processor = new RoutesRegistrationProcessor();
        java.util.Map<String, ClassInfo> classesWithRoutes = processor.collectClassesWithRoutes(index);

        List<SourcedRoute> routes = new ArrayList<>();
        for (String className : classNames) {
            ClassInfo classInfo = classesWithRoutes.get(className);
            if (classInfo == null) {
                continue;
            }
            List<RouteEntry> entries = processor.collectRoutesForClass(index, classInfo, MICROSERVICE_NAME_SENTINEL, new RouteHostMapping()).build();
            for (RouteEntry entry : entries) {
                routes.add(new SourcedRoute(className, entry));
            }
        }
        return routes;
    }

    private IndexView buildIndex(MavenProject module, File outputDirectory) throws Exception {
        List<IndexView> indexes = new ArrayList<>();
        indexes.add(indexDirectory(outputDirectory));
        for (Object element : module.getCompileClasspathElements()) {
            IndexView dependencyIndex = indexDependency(new File((String) element));
            if (dependencyIndex != null) {
                indexes.add(dependencyIndex);
            }
        }
        return CompositeIndex.create(indexes);
    }

    private IndexView indexDependency(File element) throws IOException {
        if (element.isDirectory()) {
            File jandexIdx = new File(element, "META-INF/jandex.idx");
            if (jandexIdx.exists()) {
                try (InputStream in = Files.newInputStream(jandexIdx.toPath())) {
                    return new IndexReader(in).read();
                }
            }
            if (new File(element, "META-INF/beans.xml").exists()) {
                return indexDirectory(element);
            }
            return null;
        }
        if (!element.getName().endsWith(".jar") || !element.isFile()) {
            return null;
        }
        try (ZipFile zip = new ZipFile(element)) {
            ZipEntry jandexEntry = zip.getEntry("META-INF/jandex.idx");
            if (jandexEntry != null) {
                try (InputStream in = zip.getInputStream(jandexEntry)) {
                    return new IndexReader(in).read();
                }
            }
            if (zip.getEntry("META-INF/beans.xml") != null) {
                return indexJar(zip);
            }
        }
        return null;
    }

    private IndexView indexDirectory(File directory) throws IOException {
        Indexer indexer = new Indexer();
        try (Stream<Path> files = Files.walk(directory.toPath())) {
            for (Path classFile : files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".class"))
                    .toList()) {
                try (InputStream in = Files.newInputStream(classFile)) {
                    indexer.index(in);
                }
            }
        }
        return indexer.complete();
    }

    private IndexView indexJar(ZipFile zip) throws IOException {
        Indexer indexer = new Indexer();
        var entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                try (InputStream in = zip.getInputStream(entry)) {
                    indexer.index(in);
                }
            }
        }
        return indexer.complete();
    }

    private void checkNotSpring(IndexView index, List<String> classNames) {
        for (String className : classNames) {
            ClassInfo classInfo = index.getClassByName(DotName.createSimple(className));
            if (classInfo != null && (classInfo.hasAnnotation(DotName.createSimple(SPRING_REST_CONTROLLER))
                    || classInfo.hasAnnotation(DotName.createSimple(SPRING_CONTROLLER)))) {
                throw new RouteGenerationException(
                        "Class " + className + " is a Spring controller; use httproutes-generator-spring-maven-plugin instead");
            }
        }
    }
}
