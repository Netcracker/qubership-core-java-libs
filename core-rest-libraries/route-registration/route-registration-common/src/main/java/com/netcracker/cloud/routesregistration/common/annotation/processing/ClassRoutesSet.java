package com.netcracker.cloud.routesregistration.common.annotation.processing;

import com.netcracker.cloud.routesregistration.common.annotation.FacadeRoute;
import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteEntry;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import com.netcracker.cloud.routesregistration.common.gateway.route.Utils;
import lombok.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class to hold set of routes built by all annotations in single controller class.
 * Do not use this class directly, use {@link ClassRoutesBuilder} instead.
 */
public class ClassRoutesSet {
    /**
     * Stands for microservice family name (cluster name in terms of control-plane).
     * This field is used to check if provided gateway name is a name of facade gateway.
     */
    private final String microserviceName;

    /**
     * Target route paths (prefix rewrite) obtained from class-level annotations.
     */
    private final Set<String> classWidePathsTo;
    /**
     * Gateway route paths (prefix matchers in gateway) obtained from class-level annotations.
     */
    private final Set<String> classWideGatewayPathsFrom;
    /**
     * Facade gateway route paths (prefix matchers in gateway) obtained from class-level annotations.
     * Effective only for {@code facade} gateway routes - gateway with name equals to the {@link ClassRoutesSet#microserviceName}.
     */
    private final Set<String> classWideFacadePathsFrom;
    /**
     * Information from class-level @{@link Route} annotation.
     */
    private final List<RouteAnnotationInfo> classWideRoutesAnnotationConfig;
    /**
     * Information from class-level @{@link FacadeRoute} annotation.
     */
    private final RouteAnnotationInfo classWideFacadeAnnotationConfig;

    /**
     * Creates new {@code ClassRoutesSet}.
     *
     * @param microserviceName                stands for microservice family name (cluster name in terms of control-plane).
     *                                        This field is used to check if provided gateway name is a name of facade gateway.
     * @param classWidePathsTo                target route paths (prefix rewrite) obtained from class-level annotations.
     * @param classWideGatewayPathsFrom       gateway route paths (prefix matchers in gateway) obtained from class-level annotations.
     * @param classWideFacadePathsFrom        facade gateway route paths (prefix matchers in gateway) obtained from class-level annotations.
     *                                        Effective only for {@code facade} gateway routes - gateway with name equals to the {@link ClassRoutesSet#microserviceName}.
     * @param classWideRoutesAnnotationConfig information from class-level @{@link Route} annotation.
     * @param classWideFacadeAnnotationConfig information from class-level @{@link FacadeRoute} annotation.
     */
    public ClassRoutesSet(String microserviceName,
                          Set<String> classWidePathsTo,
                          Set<String> classWideGatewayPathsFrom,
                          Set<String> classWideFacadePathsFrom,
                          List<RouteAnnotationInfo> classWideRoutesAnnotationConfig,
                          RouteAnnotationInfo classWideFacadeAnnotationConfig) {
        this.microserviceName = microserviceName;
        this.classWidePathsTo = classWidePathsTo;
        this.classWideGatewayPathsFrom = classWideGatewayPathsFrom;
        this.classWideFacadePathsFrom = classWideFacadePathsFrom;
        this.classWideRoutesAnnotationConfig = classWideRoutesAnnotationConfig;
        this.classWideFacadeAnnotationConfig = classWideFacadeAnnotationConfig;

        addClassWideRoutes();
    }

    private void addClassWideRoutes() {
        addRoutesBasedOnClassAnnotations(Collections.singleton(""), null, null);
    }

    @Getter
    private final List<RouteEntry> actualRoutes = new ArrayList<>();

    /**
     * Add routes to this class routes set based on information obtained from method-level annotations.
     * Should be called for each controller method annotated with @{@link Route}
     * or @{@link FacadeRoute} annotation.
     *
     * @param relativePathsTo       relative target paths (prefixRewrites in terms of control-plane).
     * @param routesAnnotation      information from method-level @{@link Route} annotation.
     * @param facadeRouteAnnotation information from method-level @{@link FacadeRoute} annotation.
     * @param gatewayPaths          gateways matching paths (prefix matcher in terms of control-plane) obtained from method-level annotation.
     * @param facadePaths           facade gateways matching paths (prefix matcher in terms of control-plane) obtained from method-level annotation.
     *                              Effective only for {@code facade} gateway routes - gateway with name equals to the {@link ClassRoutesSet#microserviceName}.
     */
    public void addMethodInfo(Set<String> relativePathsTo,
                              List<RouteAnnotationInfo> routesAnnotation,
                              RouteAnnotationInfo facadeRouteAnnotation,
                              Set<String> gatewayPaths,
                              Set<String> facadePaths) {
        if (routesAnnotation == null) {
            if (facadeRouteAnnotation == null) {
                addRoutesBasedOnClassAnnotations(relativePathsTo, gatewayPaths, facadePaths);
            } else {
                addRoutesBasedOnFacadeRouteAnnotation(facadeRouteAnnotation, relativePathsTo, gatewayPaths, facadePaths);
            }
        } else {
            if (facadeRouteAnnotation == null) {
                addRoutesBasedOnRouteAnnotation(routesAnnotation, relativePathsTo, gatewayPaths, facadePaths);
            } else {
                // first add routes based on method @Route annotation
                addRoutesBasedOnRouteAnnotation(routesAnnotation, relativePathsTo, gatewayPaths, facadePaths);
                // then add routes based on method @FacadeRoute annotation
                addRoutesBasedOnFacadeRouteAnnotation(facadeRouteAnnotation, relativePathsTo, gatewayPaths, facadePaths);
            }
        }
    }

    private void addRoutesBasedOnClassAnnotations(Set<String> relativePathsTo, Set<String> gatewayPaths, Set<String> facadePaths) {
        if (Utils.isEmpty(classWideRoutesAnnotationConfig)) {
            if (classWideFacadeAnnotationConfig != null) {
                addRoutesBasedOnFacadeRouteAnnotation(classWideFacadeAnnotationConfig, relativePathsTo, gatewayPaths, facadePaths);
            }
        } else {
            if (classWideFacadeAnnotationConfig == null) {
                addRoutesBasedOnRouteAnnotation(classWideRoutesAnnotationConfig, relativePathsTo, gatewayPaths, facadePaths);
            } else {
                // first add routes based on @Route
                addRoutesBasedOnRouteAnnotation(classWideRoutesAnnotationConfig, relativePathsTo, gatewayPaths, facadePaths);
                // then add routes based on @FacadeRoute
                addRoutesBasedOnFacadeRouteAnnotation(classWideFacadeAnnotationConfig, relativePathsTo, gatewayPaths, facadePaths);
            }
        }
    }

    private void addRoutesBasedOnRouteAnnotation(List<RouteAnnotationInfo> annotationsInfo, Set<String> relativePathsTo, Set<String> gatewayPaths, Set<String> facadePaths) {
        for (RouteAnnotationInfo annotationInfo : annotationsInfo) {
            if (annotationInfo.getGateways() == null || annotationInfo.getGateways().isEmpty()) {
                if (annotationInfo.getRouteType() == RouteType.FACADE) {
                    Set<RouteFromToPair> fromToPairs = resolveRouteFromToPairs(relativePathsTo, facadePaths, classWideFacadePathsFrom);
                    addFacadeGatewayRoutes(fromToPairs, annotationInfo.getHosts(), annotationInfo.getTimeout());
                } else {
                    Set<RouteFromToPair> fromToPairs = resolveRouteFromToPairs(relativePathsTo, gatewayPaths, classWideGatewayPathsFrom);
                    addBorderGatewayRoutes(fromToPairs, annotationInfo.getRouteType(), annotationInfo.getTimeout());
                }
            } else {
                addRoutesBasedOnGateways(annotationInfo, relativePathsTo, gatewayPaths, facadePaths);
            }
        }
    }

    private void addRoutesBasedOnFacadeRouteAnnotation(RouteAnnotationInfo annotationInfo, Set<String> relativePathsTo, Set<String> gatewayPaths, Set<String> facadePaths) {
        if (annotationInfo.getGateways() == null || annotationInfo.getGateways().isEmpty()) {
            Set<RouteFromToPair> fromToPairs = resolveRouteFromToPairs(relativePathsTo, facadePaths, classWideFacadePathsFrom);
            addFacadeGatewayRoutes(fromToPairs, annotationInfo.getHosts(), annotationInfo.getTimeout());
        } else {
            addRoutesBasedOnGateways(annotationInfo, relativePathsTo, gatewayPaths, facadePaths);
        }
    }

    private void addRoutesBasedOnGateways(RouteAnnotationInfo annotationInfo, Set<String> relativePathsTo, Set<String> gatewayPaths, Set<String> facadePaths) {
        annotationInfo.getGateways().forEach(gateway -> {
            if (microserviceName.equals(gateway)) {
                Set<RouteFromToPair> fromToPairs = resolveRouteFromToPairs(relativePathsTo, facadePaths, classWideFacadePathsFrom);
                addFacadeGatewayRoutes(fromToPairs, annotationInfo.getHosts(), annotationInfo.getTimeout());
            } else if (Utils.isBorderGatewayName(gateway)) {
                Set<RouteFromToPair> fromToPairs = resolveRouteFromToPairs(relativePathsTo, gatewayPaths, classWideGatewayPathsFrom);
                addBorderGatewayRoutes(fromToPairs, RouteType.fromGatewayName(gateway), annotationInfo.getTimeout());
            } else {
                Set<RouteFromToPair> fromToPairs = resolveRouteFromToPairs(relativePathsTo, gatewayPaths, classWideGatewayPathsFrom);
                addCompositeGatewayRoutes(fromToPairs, gateway, annotationInfo.getHosts(), annotationInfo.getTimeout());
            }
        });
    }

    private void addBorderGatewayRoutes(Set<RouteFromToPair> fromToPairs, RouteType routeType, Long timeout) {
        if (routeType == RouteType.FACADE) {
            throw new IllegalArgumentException("Unsupported route type for ClassRoutesSet#addRegularGatewayRoutes method: " + routeType);
        }
        fromToPairs.forEach(fromToPair -> actualRoutes.add(RouteEntry.builder()
                .from(fromToPair.getFrom())
                .to(fromToPair.getTo().getAbsolutePath())
                .type(routeType)
                .allowed(true)
                .timeout(timeout)
                .build())
        );
    }

    private void addCompositeGatewayRoutes(Set<RouteFromToPair> fromToPairs, String gateway, Set<String> hosts, Long timeout) {
        fromToPairs.forEach(fromToPair -> actualRoutes.add(RouteEntry.builder()
                .from(fromToPair.getFrom())
                .to(fromToPair.getTo().getAbsolutePath())
                .hosts(hosts)
                .type(RouteType.FACADE)
                .gateway(gateway)
                .allowed(true)
                .timeout(timeout)
                .build())
        );
    }

    private void addFacadeGatewayRoutes(Set<RouteFromToPair> fromToPairs, Set<String> hosts, Long timeout) {
        fromToPairs.forEach(fromToPair -> actualRoutes.add(RouteEntry.builder()
                .from(fromToPair.getFrom())
                .to(fromToPair.getTo().getAbsolutePath())
                .hosts(hosts)
                .type(RouteType.FACADE) // do not specify gateway field so legacy API V2 can be used
                .allowed(true)
                .timeout(timeout)
                .build())
        );
    }

    private Set<RouteFromToPair> resolveRouteFromToPairs(Set<String> methodPathsTo, Set<String> methodPathsFrom, Set<String> classPathsFrom) {
        Set<RoutePath> pathsTo = resolvePathsTo(methodPathsTo);
        return resolveRouteFromToPairsInternal(pathsTo, methodPathsFrom, classPathsFrom);
    }

    private Set<RouteFromToPair> resolveRouteFromToPairsInternal(Set<RoutePath> pathsTo, Set<String> methodPathsFrom, Set<String> classPathsFrom) {
        if (methodPathsFrom == null || methodPathsFrom.isEmpty()) {
            if (classPathsFrom == null || classPathsFrom.isEmpty()) {
                return pathsTo.stream().map(RouteFromToPair::new).collect(Collectors.toSet());
            } else {
                validatePathsNumber(pathsTo, classPathsFrom);

                Set<RouteFromToPair> result = new HashSet<>(pathsTo.size() * classPathsFrom.size());
                classPathsFrom.forEach(from ->
                        pathsTo.forEach(pathTo -> {
                            String pathFrom = from + pathTo.getRelativePath();
                            result.add(new RouteFromToPair(pathFrom, pathTo));
                        }));
                return result;
            }
        } else {
            if (classPathsFrom == null || classPathsFrom.isEmpty()) {
                validatePathsNumber(pathsTo, methodPathsFrom);

                Set<RouteFromToPair> result = new HashSet<>(pathsTo.size() * methodPathsFrom.size());
                methodPathsFrom.forEach(from ->
                        pathsTo.forEach(pathTo -> {
                            // TODO: consider using commented line in future releases (it will break backward compatibility!)
//                            String pathFrom = pathTo.getRootPath() + from;
                            String pathFrom = from;
                            result.add(new RouteFromToPair(pathFrom, pathTo));
                        }));
                return result;
            } else {
                Set<String> pathsFrom = concatenatePaths(classPathsFrom, methodPathsFrom);
                validatePathsNumber(pathsTo, pathsFrom);

                Set<RouteFromToPair> result = new HashSet<>(pathsTo.size() * pathsFrom.size());
                pathsFrom.forEach(pathFrom ->
                        pathsTo.forEach(pathTo ->
                                result.add(new RouteFromToPair(pathFrom, pathTo))));
                return result;
            }
        }
    }

    private void validatePathsNumber(Set<RoutePath> pathsTo, Set<?> pathsFrom) {
        if (pathsTo.size() > 1 && pathsFrom != null && pathsFrom.size() > 1)
            throw new RuntimeException("It's not supported to have several target paths for forwarding from different source paths!");
    }

    private Set<RoutePath> resolvePathsTo(Set<String> methodPathsTo) {
        if (classWidePathsTo == null || classWidePathsTo.isEmpty()) {
            if (methodPathsTo == null || methodPathsTo.isEmpty()) {
                return Collections.singleton(new RoutePath("", ""));
            } else {
                return methodPathsTo.stream()
                        .map(relativePath -> new RoutePath("", relativePath))
                        .collect(Collectors.toSet());
            }
        } else if (methodPathsTo == null || methodPathsTo.isEmpty()) {
            return classWidePathsTo.stream()
                    .map(rootPath -> new RoutePath(rootPath, ""))
                    .collect(Collectors.toSet());
        } else {
            Set<RoutePath> result = new HashSet<>(classWidePathsTo.size() * methodPathsTo.size());
            for (String prefix : classWidePathsTo) {
                for (String suffix : methodPathsTo) {
                    result.add(new RoutePath(prefix, suffix));
                }
            }
            return result;
        }
    }

    private Set<String> concatenatePaths(Set<String> rootPaths, Set<String> relativePaths) {
        Set<String> result = new HashSet<>(rootPaths.size() * relativePaths.size());
        for (String prefix : rootPaths) {
            for (String suffix : relativePaths) {
                result.add(prefix + suffix);
            }
        }
        return result;
    }

    @Data
    @AllArgsConstructor
    private static class RouteFromToPair {
        private String from;
        private RoutePath to;

        public RouteFromToPair(RoutePath to) {
            this.to = to;
            this.from = to.getAbsolutePath();
        }
    }

    /**
     * Represents route path (prefix or prefixRewrite in terms of control-plane).
     * This class is used in business logic when root route path and relative route path
     * are needed separately to resolve correct route pathFrom (gateway prefix matcher) value.
     */
    @Value
    @EqualsAndHashCode
    private static class RoutePath {
        String rootPath;
        String relativePath;
        String absolutePath;

        public RoutePath(String rootPath, String relativePath) {
            this.rootPath = rootPath;
            this.relativePath = relativePath;
            this.absolutePath = rootPath + relativePath;
        }
    }
}
