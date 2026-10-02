package com.netcracker.routes.gateway.plugin;

import com.netcracker.cloud.routesregistration.common.annotation.FacadeGateway;
import com.netcracker.cloud.routesregistration.common.annotation.FacadeRoute;
import com.netcracker.cloud.routesregistration.common.annotation.ForbiddenRoute;
import com.netcracker.cloud.routesregistration.common.annotation.Gateway;
import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.annotation.Routes;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.FacadeGatewayRequestMapping;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import io.github.classgraph.*;
import jakarta.ws.rs.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private static final String ROUTES_ANNOTATION = Routes.class.getName();
    private static final String FACADE_ROUTE_ANNOTATION = FacadeRoute.class.getName();
    private static final String FORBIDDEN_ROUTE_ANNOTATION = ForbiddenRoute.class.getName();
    private static final Set<String> FORBIDDEN_GATEWAY_NAMES =
            Set.of(HttpRoute.Type.PUBLIC.name(), HttpRoute.Type.PRIVATE.name());
    private static final String GATEWAY_ANNOTATION = Gateway.class.getName();
    private static final String GATEWAY_REQUEST_MAPPING = GatewayRequestMapping.class.getName();
    private static final String FACADE_GATEWAY_ANNOTATION = FacadeGateway.class.getName();
    private static final String FACADE_GATEWAY_REQUEST_MAPPING = FacadeGatewayRequestMapping.class.getName();
    private static final List<String> ROUTE_SELECTING_ANNOTATIONS = List.of(
            ROUTE_ANNOTATION, ROUTES_ANNOTATION, FACADE_ROUTE_ANNOTATION, FORBIDDEN_ROUTE_ANNOTATION
    );
    private static final Map<String, HttpRoute.Type> BORDER_GATEWAY_NAMES = Map.of(
            "public-gateway-service", HttpRoute.Type.PUBLIC,
            "private-gateway-service", HttpRoute.Type.PRIVATE,
            "internal-gateway-service", HttpRoute.Type.INTERNAL
    );

    private final String[] packages;
    private final Log log;

    public RouteScanner(String[] packages, Log log) {
        this.packages = packages;
        this.log = log;
    }

    public Set<HttpRoute> collectRoutes(List<MavenProject> reactorProjects) throws MojoExecutionException {
        return collect(reactorProjects).routes();
    }

    /**
     * @return the routes and forbidden paths of the modules, and the errors of their route annotations
     */
    public Declarations collect(List<MavenProject> reactorProjects) throws MojoExecutionException {
        Declarations declarations = new Declarations();
        for (MavenProject module : reactorProjects) {
            log.info("Scanning module: " + module.getArtifactId());
            scanModule(module, declarations);
        }
        return declarations;
    }

    Declarations collect(File classesDir) throws MojoExecutionException {
        Declarations declarations = new Declarations();
        scanClassesDir(classesDir, declarations);
        return declarations;
    }

    Declarations collect(Collection<ClassInfo> classes) {
        Declarations declarations = new Declarations();
        classes.stream()
                .filter(this::hasRoute)
                .forEach(classInfo -> collectClass(classInfo, declarations));
        return declarations;
    }

    public Set<HttpRoute> getRoutes(MavenProject module) throws MojoExecutionException {
        Declarations declarations = new Declarations();
        scanModule(module, declarations);
        return declarations.routes();
    }

    private void scanModule(MavenProject module, Declarations declarations) throws MojoExecutionException {
        scanClassesDir(new File(module.getBuild().getOutputDirectory()), declarations);
    }

    private void scanClassesDir(File classesDir, Declarations declarations) throws MojoExecutionException {
        if (!classesDir.exists()) {
            log.warn("No classes to scan: outputDirectory does not exist.");
            return;
        }

        try (ScanResult scan = createScanResult(classesDir)) {
            FrameworkType framework = detectFramework(scan);
            if (framework == FrameworkType.NONE) {
                log.info("No supported framework detected (Spring or Quarkus)");
                return;
            }

            scanClassesForRoutes(scan, framework, declarations);
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

    private void scanClassesForRoutes(ScanResult scan, FrameworkType framework, Declarations declarations) {
        getAnnotatedClasses(scan, framework)
                .distinct()
                .filter(this::hasRoute)
                .forEach(classInfo -> collectClass(classInfo, declarations));
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
        Declarations declarations = new Declarations();
        collectClass(classInfo, declarations);
        return declarations.routes();
    }

    private void collectClass(ClassInfo classInfo, Declarations declarations) {
        log.info("Get Request Mappings for Class: " + classInfo.getName());

        int before = declarations.routes().size();
        ClassContext classContext = extractClassContext(classInfo);
        for (MethodInfo methodInfo : classInfo.getMethodInfo()) {
            getHttpMappingAnnotations(methodInfo)
                    .forEach(mappingAnn -> collectMethod(classContext, methodInfo, mappingAnn, declarations));
        }
        collectClassLevel(classContext, declarations);

        if (classInfo.getSuperclass() != null) {
            collectClass(classInfo.getSuperclass(), declarations);
        }

        log.info("Found " + (declarations.routes().size() - before) + " routes");
    }

    private ClassContext extractClassContext(ClassInfo classInfo) {
        return new ClassContext(
                classInfo.getName(),
                resolveRequestMappings(classInfo),
                resolveGatewayMappings(classInfo::getAnnotationInfo, PathKind.BORDER),
                resolveGatewayMappings(classInfo::getAnnotationInfo, PathKind.FACADE),
                readRouteEntries(classInfo.getAnnotationInfo()),
                classInfo.getAnnotationInfo(FORBIDDEN_ROUTE_ANNOTATION)
        );
    }

    private void collectClassLevel(ClassContext classContext, Declarations declarations) {
        for (RouteEntry entry : classContext.routes()) {
            HttpRoute.Type type = entry.type().orElse(HttpRoute.Type.INTERNAL);
            long timeout = entry.timeout().orElse(0L);
            for (Target target : resolveTargets(entry, type)) {
                classPairs(classContext, target.pathKind()).forEach(pair -> declarations.routes().add(
                        new HttpRoute(pair.servicePath(), pair.gatewayPath(), target.type(), timeout)));
            }
        }

        readForbiddenGateways(classContext.forbiddenRoute(), classContext.name(), declarations)
                .ifPresent(gateways -> classPairs(classContext, PathKind.BORDER).forEach(pair ->
                        declarations.forbidden().add(new ForbiddenPath(pair.gatewayPath(), gateways))));
    }

    private void collectMethod(ClassContext classContext, MethodInfo methodInfo, AnnotationInfo mappingAnn, Declarations declarations) {
        List<String> mappingPaths = resolveMappingPaths(methodInfo, mappingAnn);

        for (RouteEntry entry : readRouteEntries(methodInfo.getAnnotationInfo())) {
            HttpRoute.Type type = entry.type().orElse(HttpRoute.Type.INTERNAL);
            long timeout = entry.timeout().orElse(0L);
            for (Target target : resolveTargets(entry, type)) {
                methodPairs(classContext, methodInfo, target.pathKind(), mappingPaths).forEach(pair -> declarations.routes().add(
                        new HttpRoute(pair.servicePath(), pair.gatewayPath(), target.type(), timeout)));
            }
        }

        readForbiddenGateways(methodInfo.getAnnotationInfo(FORBIDDEN_ROUTE_ANNOTATION),
                classContext.name() + "#" + methodInfo.getName(), declarations)
                .ifPresent(gateways -> methodPairs(classContext, methodInfo, PathKind.BORDER, mappingPaths).forEach(pair ->
                        declarations.forbidden().add(new ForbiddenPath(pair.gatewayPath(), gateways))));
    }

    /**
     * Gateway paths of the class itself: {@code @Gateway}-style mappings of the kind combined with the class request mappings.
     */
    private List<PathPair> classPairs(ClassContext classContext, PathKind kind) {
        List<String> gatewayMappings = classContext.gatewayMappings(kind);
        List<String> requestMappings = classContext.requestMappings();
        if (requestMappings.isEmpty() && gatewayMappings.isEmpty()) {
            return List.of();
        }
        if (!gatewayMappings.isEmpty()) {
            return buildClassGatewayPairs(gatewayMappings, List.of(), requestMappings, List.of(""));
        }
        return requestMappings.stream().map(path -> new PathPair(path, path)).toList();
    }

    private List<PathPair> methodPairs(ClassContext classContext, MethodInfo methodInfo, PathKind kind, List<String> mappingPaths) {
        List<String> methodGatewayMappings = resolveGatewayMappings(methodInfo::getAnnotationInfo, kind);
        if (!classContext.gatewayMappings(kind).isEmpty()) {
            return buildClassGatewayPairs(
                    classContext.gatewayMappings(kind),
                    methodGatewayMappings,
                    classContext.requestMappings(),
                    mappingPaths
            );
        }
        if (!methodGatewayMappings.isEmpty()) {
            return buildMethodGatewayPairs(methodGatewayMappings, classContext.requestMappings(), mappingPaths);
        }
        return buildStandardPairs(classContext.requestMappings(), mappingPaths);
    }

    /**
     * Legacy resolution of one route annotation into targets: empty {@code gateways} goes by type, a border gateway name
     * gives a border route of that gateway's type, and every other name is a composite gateway.
     */
    private List<Target> resolveTargets(RouteEntry entry, HttpRoute.Type type) {
        if (entry.gateways().isEmpty()) {
            return List.of(type == HttpRoute.Type.FACADE
                    ? new Target(HttpRoute.Type.FACADE, PathKind.FACADE)
                    : new Target(type, PathKind.BORDER));
        }
        return entry.gateways().stream()
                .map(name -> Optional.ofNullable(BORDER_GATEWAY_NAMES.get(name))
                        .map(borderType -> new Target(borderType, PathKind.BORDER))
                        .orElse(new Target(HttpRoute.Type.FACADE, PathKind.BORDER)))
                .distinct()
                .toList();
    }

    private Optional<Set<HttpRoute.Type>> readForbiddenGateways(AnnotationInfo forbiddenRoute, String element, Declarations declarations) {
        if (forbiddenRoute == null) {
            return Optional.empty();
        }
        List<String> names = enumValueNames(forbiddenRoute.getParameterValues(false).getValue("value"));
        if (names.isEmpty() || !FORBIDDEN_GATEWAY_NAMES.containsAll(names)) {
            declarations.errors().add("@ForbiddenRoute of " + element + " must list PUBLIC and/or PRIVATE, found " + names);
            return Optional.empty();
        }
        return Optional.of(names.stream().map(HttpRoute.Type::valueOf).collect(Collectors.toCollection(() -> EnumSet.noneOf(HttpRoute.Type.class))));
    }

    /**
     * Reads every route annotation of an element: each {@code @Route} (classgraph may unwrap the {@code @Routes}
     * container into several of them), the {@code @Routes} container when it is not unwrapped, and {@code @FacadeRoute}.
     */
    private List<RouteEntry> readRouteEntries(AnnotationInfoList annotations) {
        List<RouteEntry> entries = new ArrayList<>();
        for (AnnotationInfo annotation : annotations) {
            String name = annotation.getName();
            if (ROUTE_ANNOTATION.equals(name)) {
                entries.add(routeEntry(annotation));
            } else if (ROUTES_ANNOTATION.equals(name)
                    && annotation.getParameterValues(false).getValue("value") instanceof Object[] values) {
                Arrays.stream(values)
                        .filter(AnnotationInfo.class::isInstance)
                        .map(AnnotationInfo.class::cast)
                        .map(this::routeEntry)
                        .forEach(entries::add);
            } else if (FACADE_ROUTE_ANNOTATION.equals(name)) {
                entries.add(new RouteEntry(
                        Optional.of(HttpRoute.Type.FACADE),
                        getRouteTimeout(annotation),
                        stringValues(annotation.getParameterValues(false).getValue("gateways"))
                ));
            }
        }
        return entries;
    }

    private RouteEntry routeEntry(AnnotationInfo route) {
        AnnotationParameterValueList parameters = route.getParameterValues(false);
        return new RouteEntry(
                getRouteType(route),
                getRouteTimeout(route),
                stringValues(parameters.getValue("gateways"))
        );
    }

    private static List<String> stringValues(Object value) {
        Stream<?> values = switch (value) {
            case null -> Stream.empty();
            case Object[] objects -> Arrays.stream(objects);
            default -> Stream.of(value);
        };
        return values
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private static List<String> enumValueNames(Object value) {
        Stream<?> values = switch (value) {
            case null -> Stream.empty();
            case Object[] objects -> Arrays.stream(objects);
            default -> Stream.of(value);
        };
        return values
                .filter(AnnotationEnumValue.class::isInstance)
                .map(AnnotationEnumValue.class::cast)
                .map(AnnotationEnumValue::getValueName)
                .toList();
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
        return ROUTE_SELECTING_ANNOTATIONS.stream()
                .anyMatch(annotation -> classInfo.hasAnnotation(annotation) || classInfo.hasMethodAnnotation(annotation));
    }

    private boolean isRequestMappingAnnotation(AnnotationInfo annotationInfo) {
        return RequestMapping.class.getName().equals(annotationInfo.getName());
    }

    private List<String> resolveGatewayMappings(Function<String, AnnotationInfo> annotations, PathKind kind) {
        String requestMapping = kind == PathKind.BORDER ? GATEWAY_REQUEST_MAPPING : FACADE_GATEWAY_REQUEST_MAPPING;
        String gateway = kind == PathKind.BORDER ? GATEWAY_ANNOTATION : FACADE_GATEWAY_ANNOTATION;
        AnnotationInfo requestMappingInfo = annotations.apply(requestMapping);
        if (requestMappingInfo != null) {
            return getAnnotationPathFor(requestMappingInfo);
        }
        return getAnnotationPathFor(annotations.apply(gateway));
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

    private List<String> resolveMappingPaths(MethodInfo methodInfo, AnnotationInfo mappingAnn) {
        if (JAX_RS_HTTP_ANNOTATIONS.stream().map(Class::getName).anyMatch(s -> s.equals(mappingAnn.getClassInfo().getName()))) {
            List<String> paths = getAnnotationPathFor(methodInfo.getAnnotationInfo(Path.class.getName()));
            return paths.isEmpty() ? List.of("") : paths;
        }
        return getAnnotationPathFor(mappingAnn);
    }

    private List<PathPair> buildStandardPairs(List<String> classMappings, List<String> methodMappings) {
        if (classMappings.isEmpty()) {
            return methodMappings.stream()
                    .map(path -> new PathPair(path, path))
                    .toList();
        }

        return classMappings.stream()
                .flatMap(classPrefix -> methodMappings.stream()
                        .map(methodPath -> new PathPair(classPrefix + methodPath, classPrefix + methodPath)))
                .toList();
    }

    private List<PathPair> buildClassGatewayPairs(
            List<String> classGatewayMappings,
            List<String> methodGatewayMappings,
            List<String> classMappings,
            List<String> methodMappings
    ) {
        List<String> effectiveMethodMappings = methodMappings.isEmpty() ? List.of("/") : methodMappings;
        List<String> effectiveMethodGatewayMappings = methodGatewayMappings.isEmpty()
                ? effectiveMethodMappings
                : methodGatewayMappings;

        String servicePrefix = classMappings.isEmpty() ? "" : classMappings.get(0);
        String mappingPath = effectiveMethodMappings.get(0);

        return classGatewayMappings.stream()
                .flatMap(classPrefix -> effectiveMethodGatewayMappings.stream()
                        .map(methodPath -> new PathPair(servicePrefix + mappingPath, classPrefix + methodPath)))
                .toList();
    }

    private List<PathPair> buildMethodGatewayPairs(
            List<String> methodGatewayMappings,
            List<String> classMappings,
            List<String> methodMappings
    ) {
        if (methodGatewayMappings.isEmpty() || methodMappings.isEmpty()) {
            return List.of();
        }

        String servicePrefix = classMappings.isEmpty() ? "" : classMappings.get(0);
        String mappingPath = methodMappings.get(0);

        return methodGatewayMappings.stream()
                .map(methodPath -> new PathPair(servicePrefix + mappingPath, methodPath))
                .toList();
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
                .flatMap(params -> Optional.ofNullable(params.getValue("value"))
                        .filter(value -> !(value instanceof AnnotationEnumValue e
                                && e.getValueName().equals(HttpRoute.Type.INTERNAL.name())))
                        .or(() -> Optional.ofNullable(params.getValue("type"))))
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

    /**
     * Which gateway path annotations apply: {@code @Gateway}/{@code @GatewayRequestMapping} for border and composite
     * routes, {@code @FacadeGateway}/{@code @FacadeGatewayRequestMapping} for facade routes.
     */
    private enum PathKind {
        BORDER, FACADE
    }

    private record PathPair(String servicePath, String gatewayPath) {}

    private record Target(HttpRoute.Type type, PathKind pathKind) {}

    private record RouteEntry(
            Optional<HttpRoute.Type> type,
            Optional<Long> timeout,
            List<String> gateways
    ) {}

    private record ClassContext(
            String name,
            List<String> requestMappings,
            List<String> borderGatewayMappings,
            List<String> facadeGatewayMappings,
            List<RouteEntry> routes,
            AnnotationInfo forbiddenRoute
    ) {
        List<String> gatewayMappings(PathKind kind) {
            return kind == PathKind.BORDER ? borderGatewayMappings : facadeGatewayMappings;
        }
    }

    /**
     * @param errors errors of the route annotations
     */
    public record Declarations(Set<HttpRoute> routes, Set<ForbiddenPath> forbidden, List<String> errors) {

        Declarations() {
            this(new HashSet<>(), new HashSet<>(), new ArrayList<>());
        }
    }
}
