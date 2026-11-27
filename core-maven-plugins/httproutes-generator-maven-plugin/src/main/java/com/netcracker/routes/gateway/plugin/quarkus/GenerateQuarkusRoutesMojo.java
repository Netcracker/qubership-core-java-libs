package com.netcracker.routes.gateway.plugin.quarkus;

import com.netcracker.cloud.quarkus.routesregistration.deployment.RoutesRegistrationProcessor;
import com.netcracker.cloud.routesregistration.common.annotation.processing.RouteHostMapping;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteEntry;
import com.netcracker.routes.gateway.plugin.AbstractGenerateRoutesMojo;
import com.netcracker.routes.gateway.plugin.FrameworkMismatch;
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
import java.util.Map;
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

    private static final DotName SPRING_REST_CONTROLLER = DotName.createSimple(FrameworkMismatch.SPRING_REST_CONTROLLER);
    private static final DotName SPRING_CONTROLLER = DotName.createSimple(FrameworkMismatch.SPRING_CONTROLLER);
    private static final DotName JAKARTA_WS_RS_PATH = DotName.createSimple(FrameworkMismatch.JAKARTA_WS_RS_PATH);
    private static final DotName JAVAX_WS_RS_PATH = DotName.createSimple(FrameworkMismatch.JAVAX_WS_RS_PATH);

    @Override
    protected boolean isQuarkus() {
        return true;
    }

    @Override
    protected List<SourcedRoute> collect(MavenProject module) throws Exception {
        File outputDirectory = new File(module.getBuild().getOutputDirectory());
        List<String> classNames = Rules.classNames(outputDirectory, packages);
        if (classNames.isEmpty()) {
            return List.of();
        }

        IndexView index = buildIndex(module, outputDirectory);
        recordFrameworkMarkers(index, classNames);

        RoutesRegistrationProcessor processor = new RoutesRegistrationProcessor();
        Map<String, ClassInfo> classesWithRoutes = processor.collectClassesWithRoutes(index);

        List<SourcedRoute> routes = new ArrayList<>();
        for (String className : classNames) {
            ClassInfo classInfo = classesWithRoutes.get(className);
            if (classInfo != null) {
                for (RouteEntry entry : processor.collectRoutesForClass(index, classInfo, MICROSERVICE_NAME_SENTINEL, new RouteHostMapping()).build()) {
                    routes.add(new SourcedRoute(className, entry));
                }
            }
        }
        return routes;
    }

    private void recordFrameworkMarkers(IndexView index, List<String> classNames) {
        for (String className : classNames) {
            ClassInfo classInfo = index.getClassByName(DotName.createSimple(className));
            if (classInfo == null) {
                continue;
            }
            if (classInfo.hasAnnotation(SPRING_REST_CONTROLLER) || classInfo.hasAnnotation(SPRING_CONTROLLER)) {
                sawSpringMarkers = true;
            }
            if (classInfo.hasAnnotation(JAKARTA_WS_RS_PATH) || classInfo.hasAnnotation(JAVAX_WS_RS_PATH)
                    || classInfo.methods().stream().anyMatch(m -> m.hasAnnotation(JAKARTA_WS_RS_PATH) || m.hasAnnotation(JAVAX_WS_RS_PATH))) {
                sawQuarkusMarkers = true;
            }
        }
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
            return new File(element, "META-INF/beans.xml").exists() ? indexDirectory(element) : null;
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
            return zip.getEntry("META-INF/beans.xml") != null ? indexJar(zip) : null;
        }
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
        for (ZipEntry entry : zip.stream().filter(e -> !e.isDirectory() && e.getName().endsWith(".class")).toList()) {
            try (InputStream in = zip.getInputStream(entry)) {
                indexer.index(in);
            }
        }
        return indexer.complete();
    }
}
