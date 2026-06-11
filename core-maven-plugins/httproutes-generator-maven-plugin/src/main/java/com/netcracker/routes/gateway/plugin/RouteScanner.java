package com.netcracker.routes.gateway.plugin;

import com.netcracker.cloud.routesregistration.common.annotation.Gateway;
import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import io.github.classgraph.*;
import jakarta.ws.rs.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

public class RouteScanner {

    private static final Set<Class<?>> SPRING_HTTP_ANNOTATIONS = Set.of(
            RequestMapping.class, GetMapping.class, PostMapping.class,
            PutMapping.class, DeleteMapping.class, PatchMapping.class
    );

    private static final Set<Class<?>> JAX_RS_HTTP_ANNOTATIONS = Set.of(
            GET.class, POST.class, PUT.class, DELETE.class, PATCH.class
    );

    private static final String ROUTE_ANNOTATION = Route.class.getName();
    private static final String GATEWAY_ANNOTATION = Gateway.class.getName();
    private static final String GATEWAY_REQUEST_MAPPING = GatewayRequestMapping.class.getName();

    private final String[] packages;
    private final Log log;

    public RouteScanner(String[] packages, Log log) {
        this.packages = packages;
        this.log = log;
    }

    public Set<HttpRoute> collectRoutes(List<MavenProject> reactorProjects) throws MojoExecutionException {
        Set<HttpRoute> allRoutes = new HashSet<>();
        for (MavenProject module : reactorProjects) {
            log.info("Scanning module: " + module.getArtifactId());
            allRoutes.addAll(getRoutes(module));
        }
        return allRoutes;
    }

    public Set<HttpRoute> getRoutes(MavenProject module) throws MojoExecutionException {
        File classesDir = new File(module.getBuild().getOutputDirectory());
        if (!classesDir.exists()) {
            log.warn("No classes to scan: outputDirectory does not exist.");
            return Collections.emptySet();
        }

        try (ScanResult scan = createScanResult(classesDir)) {
            FrameworkType framework = detectFramework(scan);
            if (framework == FrameworkType.NONE) {
                log.info("No supported framework detected (Spring or Quarkus)");
                return Collections.emptySet();
            }

            return scanClassesForRoutes(scan, framework);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed scanning annotations", e);
        }
    }

    private ScanResult createScanResult(File classesDir) {
        return new ClassGraph()
                .enableAllInfo()
                .overrideClasspath(classesDir.getAbsolutePath())
                .acceptPackages(packages)
                .disableRuntimeInvisibleAnnotations()
                .scan();
    }

    private FrameworkType detectFramework(ScanResult scan) {
        if (isSpringUsed(scan)) {
            return FrameworkType.SPRING;
        }
        if (isQuarkusUsed(scan)) {
            return FrameworkType.QUARKUS;
        }
        return FrameworkType.NONE;
    }

    private Set<HttpRoute> scanClassesForRoutes(ScanResult scan, FrameworkType framework) {
        return getAnnotatedClasses(scan, framework)
                .distinct()
                .filter(this::hasRoute)
                .flatMap(classInfo -> getRequestMappingPaths(classInfo).stream())
                .collect(java.util.stream.Collectors.toSet());
    }

    private Stream<ClassInfo> getAnnotatedClasses(ScanResult scan, FrameworkType framework) {
        Set<Class<?>> annotations = framework == FrameworkType.SPRING
                ? SPRING_HTTP_ANNOTATIONS
                : JAX_RS_HTTP_ANNOTATIONS;

        if (framework == FrameworkType.QUARKUS) {
            return Stream.concat(
                    getClassesWithAnnotations(scan, Set.of(Path.class)),
                    getClassesWithAnnotations(scan, annotations)
            );
        }

        return getClassesWithAnnotations(scan, annotations);
    }

    private Stream<ClassInfo> getClassesWithAnnotations(ScanResult scan, Set<Class<?>> annotations) {
        return annotations.stream()
                .flatMap(annotation -> Stream.concat(
                        scan.getClassesWithMethodAnnotation(annotation.getName()).stream(),
                        scan.getClassesWithAnnotation(annotation.getName()).stream()
                ));
    }

    public Set<HttpRoute> getRequestMappingPaths(ClassInfo classInfo) {
        log.info("Get Request Mappings for Class: " + classInfo.getName());

        RouteMetadata classMetadata = extractClassMetadata(classInfo);
        Set<HttpRoute> routes = processMethodRoutes(classInfo, classMetadata);
        routes.addAll(buildClassLevelRoutes(classInfo, classMetadata));

        if (classInfo.getSuperclass() != null) {
            routes.addAll(getRequestMappingPaths(classInfo.getSuperclass()));
        }

        log.info("Found " + routes.size() + " routes");
        return routes;
    }

    private RouteMetadata extractClassMetadata(ClassInfo classInfo) {
        return new RouteMetadata(
                getRouteType(classInfo.getAnnotationInfo(ROUTE_ANNOTATION)),
                getRouteTimeout(classInfo.getAnnotationInfo(ROUTE_ANNOTATION)),
                resolveGatewayMappings(classInfo),
                resolveRequestMappings(classInfo)
        );
    }

    private Set<HttpRoute> processMethodRoutes(ClassInfo classInfo, RouteMetadata classMetadata) {
        return classInfo.getMethodInfo().stream()
                .flatMap(methodInfo -> getHttpMappingAnnotations(methodInfo)
                        .map(mappingAnn -> Map.entry(methodInfo, mappingAnn)))
                .flatMap(entry -> buildRoutesForMethod(
                        entry.getKey(),
                        entry.getValue(),
                        classMetadata
                ).stream())
                .collect(java.util.stream.Collectors.toSet());
    }

    private Set<HttpRoute> buildClassLevelRoutes(ClassInfo classInfo, RouteMetadata classMetadata) {
        if (!classInfo.hasAnnotation(ROUTE_ANNOTATION)) {
            return Collections.emptySet();
        }

        if (classMetadata.requestMappings().isEmpty() && classMetadata.gatewayMappings().isEmpty()) {
            return Collections.emptySet();
        }

        HttpRoute.Type routeType = classMetadata.routeType().orElse(HttpRoute.Type.INTERNAL);
        long routeTimeout = classMetadata.routeTimeout().orElse(0L);

        if (!classMetadata.gatewayMappings().isEmpty()) {
            return buildClassGatewayRoutes(
                    classMetadata.gatewayMappings(),
                    List.of(),
                    classMetadata.requestMappings(),
                    List.of(""),
                    routeType,
                    routeTimeout
            );
        }

        return classMetadata.requestMappings().stream()
                .map(path -> new HttpRoute(path, routeType, routeTimeout))
                .collect(java.util.stream.Collectors.toSet());
    }

    private boolean hasMethodRouteAnnotation(MethodInfo methodInfo) {
        return methodInfo.hasAnnotation(ROUTE_ANNOTATION);
    }

    private Stream<AnnotationInfo> getHttpMappingAnnotations(MethodInfo methodInfo) {
        AnnotationInfoList annotations = methodInfo.getAnnotationInfo();

        List<AnnotationInfo> specificMappings = annotations.stream()
                .filter(this::isHttpMappingAnnotation)
                .toList();

        if (!specificMappings.isEmpty()) {
            return specificMappings.stream();
        }

        return annotations.stream()
                .filter(this::isRequestMappingAnnotation);
    }

    private boolean hasRoute(ClassInfo classInfo) {
        return classInfo.hasAnnotation(Route.class) || classInfo.hasMethodAnnotation(Route.class);
    }

    private boolean isRequestMappingAnnotation(AnnotationInfo annotationInfo) {
        return RequestMapping.class.getName().equals(annotationInfo.getName());
    }

    private List<String> resolveGatewayMappings(ClassInfo classInfo) {
        if (classInfo.hasAnnotation(GATEWAY_REQUEST_MAPPING)) {
            return getAnnotationPathFor(classInfo.getAnnotationInfo(GATEWAY_REQUEST_MAPPING));
        }
        return getAnnotationPathFor(classInfo.getAnnotationInfo(GATEWAY_ANNOTATION));
    }

    private List<String> resolveGatewayMappings(MethodInfo methodInfo) {
        if (methodInfo.hasAnnotation(GATEWAY_REQUEST_MAPPING)) {
            return getAnnotationPathFor(methodInfo.getAnnotationInfo(GATEWAY_REQUEST_MAPPING));
        }
        return getAnnotationPathFor(methodInfo.getAnnotationInfo(GATEWAY_ANNOTATION));
    }

    private List<String> resolveRequestMappings(ClassInfo classInfo) {
        return Stream.of(
                        RequestMapping.class, GetMapping.class, PostMapping.class,
                        PutMapping.class, DeleteMapping.class, PatchMapping.class, Path.class
                )
                .map(Class::getName)
                .map(classInfo::getAnnotationInfo)
                .filter(Objects::nonNull)
                .findFirst()
                .map(this::getAnnotationPathFor)
                .orElse(Collections.emptyList());
    }

    private boolean isHttpMappingAnnotation(AnnotationInfo annotationInfo) {
        String name = annotationInfo.getName();
        return GetMapping.class.getName().equals(name) ||
                PostMapping.class.getName().equals(name) ||
                PutMapping.class.getName().equals(name) ||
                DeleteMapping.class.getName().equals(name) ||
                PatchMapping.class.getName().equals(name) ||
                GET.class.getName().equals(name) ||
                POST.class.getName().equals(name) ||
                PUT.class.getName().equals(name) ||
                DELETE.class.getName().equals(name) ||
                PATCH.class.getName().equals(name);
    }

    private Set<HttpRoute> buildRoutesForMethod(
            MethodInfo methodInfo,
            AnnotationInfo mappingAnn,
            RouteMetadata classMetadata
    ) {
        if (!hasMethodRouteAnnotation(methodInfo)) {
            return Collections.emptySet();
        }

        HttpRoute.Type routeType = getRouteType(methodInfo.getAnnotationInfo(ROUTE_ANNOTATION))
                .orElse(classMetadata.routeType().orElse(HttpRoute.Type.INTERNAL));

        long routeTimeout = getRouteTimeout(methodInfo.getAnnotationInfo(ROUTE_ANNOTATION))
                .orElse(classMetadata.routeTimeout().orElse(0L));

        List<String> methodGatewayMappings = resolveGatewayMappings(methodInfo);
        List<String> mappingPaths = resolveMappingPaths(methodInfo, mappingAnn);

        if (!classMetadata.gatewayMappings().isEmpty()) {
            return buildClassGatewayRoutes(
                    classMetadata.gatewayMappings(),
                    methodGatewayMappings,
                    classMetadata.requestMappings(),
                    mappingPaths,
                    routeType,
                    routeTimeout
            );
        }

        if (!methodGatewayMappings.isEmpty()) {
            return buildMethodGatewayRoutes(
                    methodGatewayMappings,
                    classMetadata.requestMappings(),
                    mappingPaths,
                    routeType,
                    routeTimeout
            );
        }

        return buildStandardRoutes(classMetadata.requestMappings(), mappingPaths, routeType, routeTimeout);
    }

    private List<String> resolveMappingPaths(MethodInfo methodInfo, AnnotationInfo mappingAnn) {
        if (JAX_RS_HTTP_ANNOTATIONS.stream().map(Class::getName).anyMatch(s -> s.equals(mappingAnn.getClassInfo().getName()))) {
            List<String> paths = getAnnotationPathFor(methodInfo.getAnnotationInfo(Path.class.getName()));
            return paths.isEmpty() ? List.of("") : paths;
        }
        return getAnnotationPathFor(mappingAnn);
    }

    private Set<HttpRoute> buildStandardRoutes(
            List<String> classMappings,
            List<String> methodMappings,
            HttpRoute.Type routeType,
            long routeTimeout
    ) {
        if (classMappings.isEmpty()) {
            return methodMappings.stream()
                    .map(path -> new HttpRoute(path, routeType, routeTimeout))
                    .collect(java.util.stream.Collectors.toSet());
        }

        return classMappings.stream()
                .flatMap(classPrefix -> methodMappings.stream()
                        .map(methodPath -> new HttpRoute(classPrefix + methodPath, routeType, routeTimeout)))
                .collect(java.util.stream.Collectors.toSet());
    }

    private Set<HttpRoute> buildClassGatewayRoutes(
            List<String> classGatewayMappings,
            List<String> methodGatewayMappings,
            List<String> classMappings,
            List<String> methodMappings,
            HttpRoute.Type routeType,
            long routeTimeout
    ) {
        List<String> effectiveMethodMappings = methodMappings.isEmpty() ? List.of("/") : methodMappings;
        List<String> effectiveMethodGatewayMappings = methodGatewayMappings.isEmpty()
                ? effectiveMethodMappings
                : methodGatewayMappings;

        String servicePrefix = classMappings.isEmpty() ? "" : classMappings.get(0);
        String mappingPath = effectiveMethodMappings.get(0);

        return classGatewayMappings.stream()
                .flatMap(classPrefix -> effectiveMethodGatewayMappings.stream()
                        .map(methodPath -> new HttpRoute(
                                servicePrefix + mappingPath,
                                classPrefix + methodPath,
                                routeType,
                                routeTimeout
                        )))
                .collect(java.util.stream.Collectors.toSet());
    }

    private Set<HttpRoute> buildMethodGatewayRoutes(
            List<String> methodGatewayMappings,
            List<String> classMappings,
            List<String> methodMappings,
            HttpRoute.Type routeType,
            long routeTimeout
    ) {
        if (methodGatewayMappings.isEmpty() || methodMappings.isEmpty()) {
            return Collections.emptySet();
        }

        String servicePrefix = classMappings.isEmpty() ? "" : classMappings.get(0);
        String mappingPath = methodMappings.get(0);

        return methodGatewayMappings.stream()
                .map(methodPath -> new HttpRoute(
                        servicePrefix + mappingPath,
                        methodPath,
                        routeType,
                        routeTimeout
                ))
                .collect(java.util.stream.Collectors.toSet());
    }

    private List<String> getAnnotationPathFor(AnnotationInfo annotationInfo) {
        if (annotationInfo == null) {
            return Collections.emptyList();
        }

        AnnotationParameterValueList parameters = annotationInfo.getParameterValues();
        Object valueParam = parameters.getValue("value");
        Object pathParam = parameters.getValue("path");

        if (isNullOrEmpty(valueParam) && isNullOrEmpty(pathParam)) {
            return List.of("");
        }

        if (valueParam instanceof String && !isNullOrEmpty(valueParam)) {
            return List.of(valueParam.toString());
        }
        if (pathParam instanceof String && !isNullOrEmpty(pathParam)) {
            return List.of(pathParam.toString());
        }

        return extractPathsFromParameter(parameters, "value")
                .or(() -> extractPathsFromParameter(parameters, "path"))
                .orElse(List.of(""));
    }

    private boolean isNullOrEmpty(Object param) {
        return switch (param) {
            case null -> true;
            case String s -> s.isEmpty();
            case Object[] objects -> objects.length == 0;
            default -> false;
        };
    }

    private Optional<List<String>> extractPathsFromParameter(AnnotationParameterValueList parameters, String parameterName) {
        Object paramValue = parameters.getValue(parameterName);

        return switch (paramValue) {
            case null -> Optional.empty();
            case String s -> Optional.of(List.of(s));
            case Object[] objects -> {
                if (objects.length == 0) {
                    yield Optional.empty();
                }
                List<String> paths = Arrays.stream(objects)
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .toList();
                yield paths.isEmpty() ? Optional.empty() : Optional.of(paths);
            }
            default -> Optional.empty();
        };
    }

    private Optional<Long> getRouteTimeout(AnnotationInfo annotationInfo) {
        return Optional.ofNullable(annotationInfo)
                .map(a -> a.getParameterValues(false))
                .map(p -> p.getValue("timeout"))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::longValue);
    }

    private Optional<HttpRoute.Type> getRouteType(AnnotationInfo annotationInfo) {
        return Optional.ofNullable(annotationInfo)
                .map(annInfo -> annInfo.getParameterValues(false))
                .flatMap(params ->
                        Optional.ofNullable(params.getValue("type"))
                                .or(() -> Optional.ofNullable(params.getValue("value")))
                )
                .filter(AnnotationEnumValue.class::isInstance)
                .map(AnnotationEnumValue.class::cast)
                .map(enumVal -> HttpRoute.Type.valueOf(enumVal.getValueName()));
    }

    private boolean isSpringUsed(ScanResult scan) {
        return SPRING_HTTP_ANNOTATIONS.stream()
                .anyMatch(annotation ->
                        !scan.getClassesWithMethodAnnotation(annotation.getName()).isEmpty() ||
                                !scan.getClassesWithAnnotation(annotation.getName()).isEmpty()
                );
    }

    private boolean isQuarkusUsed(ScanResult scan) {
        return !scan.getClassesWithMethodAnnotation(Path.class).isEmpty() ||
                !scan.getClassesWithAnnotation(Path.class).isEmpty();
    }

    private enum FrameworkType {
        SPRING, QUARKUS, NONE
    }

    private record RouteMetadata(
            Optional<HttpRoute.Type> routeType,
            Optional<Long> routeTimeout,
            List<String> gatewayMappings,
            List<String> requestMappings
    ) {}
}
