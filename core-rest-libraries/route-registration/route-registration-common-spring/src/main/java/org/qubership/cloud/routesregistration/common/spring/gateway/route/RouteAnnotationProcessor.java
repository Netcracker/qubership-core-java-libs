package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.qubership.cloud.routesregistration.common.annotation.FacadeRoute;
import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.qubership.cloud.routesregistration.common.annotation.Routes;
import org.qubership.cloud.routesregistration.common.annotation.processing.ClassRoutesBuilder;
import org.qubership.cloud.routesregistration.common.annotation.processing.MethodRoutesBuilder;
import org.qubership.cloud.routesregistration.common.annotation.processing.MicroserviceRoutesBuilder;
import org.qubership.cloud.routesregistration.common.annotation.processing.RouteHostMapping;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteEntry;
import org.qubership.cloud.routesregistration.common.spring.gateway.route.annotation.FacadeGatewayRequestMapping;
import org.qubership.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.qubership.cloud.routesregistration.common.spring.gateway.route.RouteAnnotationProcessorUtil.*;

/**
 * Class to collect routes based on {@link Routes}, {@link Route}, {@link FacadeRoute},
 * {@link GatewayRequestMapping} and {@link FacadeGatewayRequestMapping} annotations.
 */
@Slf4j
@RequiredArgsConstructor
public class RouteAnnotationProcessor {

    private static final Set<String> DEFAULT_SET = Collections.singleton("");

    private final RouteFormatter routeFormatter;
    private final ApplicationContext applicationContext;
    private final RouteHostMapping routeHostMapping;
    @Value("${cloud.microservice.name}")
    private String microserviceName;

    public Collection<RouteEntry> scanForRoutes() {
        MicroserviceRoutesBuilder microserviceRoutesBuilder = new MicroserviceRoutesBuilder();
        Arrays.stream(applicationContext.getBeanDefinitionNames())
                .map(applicationContext::getType)
                .filter(Objects::nonNull)
                .filter(beanClass -> RouteAnnotationProcessorUtil.isAnnotatedWithRoute(beanClass)
                        || RouteAnnotationProcessorUtil.isAnnotatedWithFacadeRoute(beanClass))
                .map(routeAnnotatedClass -> {
                    try {
                        return getRoutesForClass(routeAnnotatedClass);
                    } catch (Exception e) {
                        log.error("Error get Route data from classes:", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(microserviceRoutesBuilder::withClass);
        return microserviceRoutesBuilder.build();
    }

    public Collection<RouteEntry> scanForRoutes(Class<?> beanClass) throws Exception {
        return new MicroserviceRoutesBuilder().withClass(getRoutesForClass(beanClass)).build();
    }

    /**
     * {@code List<RouteEntry>}-based API is kept for backward compatibility.
     * Please, prefer {@code Collection<RouteEntry>}-based API for better performance.
     *
     * @return RouteEntries set transformed to {@code List}.
     */
    @Deprecated
    public List<RouteEntry> getRouteEntries() {
        return new ArrayList<>(scanForRoutes());
    }

    /**
     * {@code List<RouteEntry>}-based API is kept for backward compatibility.
     * Please, prefer {@code Collection<RouteEntry>}-based API for better performance.
     *
     * @return RouteEntries set transformed to {@code List}.
     */
    @Deprecated
    public List<RouteEntry> getRouteEntries(Class<?> beanClass) throws Exception {
        return new ArrayList<>(scanForRoutes(beanClass));
    }

    private ClassRoutesBuilder getRoutesForClass(Class<?> beanClass) throws Exception {
        log.debug("Routes scanning for class {} started.", beanClass.getName());
        ClassRoutesBuilder classRoutesBuilder;
        try {
            classRoutesBuilder = processClassWideAnnotations(beanClass);

            ReflectionUtils.doWithMethods(beanClass,
                    method -> processRoutedMethod(method, classRoutesBuilder),
                    method -> RouteAnnotationProcessorUtil.isAnnotatedWithRoute(method) || RouteAnnotationProcessorUtil.isAnnotatedWithFacadeRoute(method));
        } catch (Exception e) {
            log.error("Error during Route annotations collecting:", e);
            throw new Exception(e);
        }
        log.debug("Routes scanning for class {} completed successfully.", beanClass.getName());
        return classRoutesBuilder;
    }

    private ClassRoutesBuilder processClassWideAnnotations(Class<?> beanClass) {
        ClassRoutesBuilder classRoutesBuilder = new ClassRoutesBuilder(microserviceName, routeHostMapping);

        Optional<Routes> routesAnnotation = getAnnotationFromClass(beanClass, Routes.class);
        routesAnnotation.ifPresent(classRoutesBuilder::withRouteAnnotation);

        Optional<Route> routeAnnotation = getAnnotationFromClass(beanClass, Route.class);
        routeAnnotation.ifPresent(classRoutesBuilder::withRouteAnnotation);

        Optional<FacadeRoute> facadeRouteAnnotation = getAnnotationFromClass(beanClass, FacadeRoute.class);
        facadeRouteAnnotation.ifPresent(classRoutesBuilder::withFacadeRouteAnnotation);

        Set<String> classPathsFrom = getGatewayRequestMappingPaths(beanClass);
        classRoutesBuilder.withGatewayPathsFrom(classPathsFrom);

        Set<String> classFacadePathsFrom = getFacadeGatewayRequestMappingPaths(beanClass);
        classRoutesBuilder.withFacadeGatewayPathsFrom(classFacadePathsFrom);

        Set<String> classPathsTo = getRequestMappingPaths(beanClass);
        classRoutesBuilder.withPathsTo(classPathsTo);

        return classRoutesBuilder;
    }

    private Set<String> getGatewayRequestMappingPaths(Class<?> routedClass) {
        Optional<GatewayRequestMapping> classRequestMapping = getAnnotationFromClass(routedClass, GatewayRequestMapping.class);
        return or(classRequestMapping.map(a -> getNonEmptyValue(a.value())),
                classRequestMapping.map(a -> getNonEmptyValue(a.path())))
                .orElse(null);
    }

    private Set<String> getRequestMappingPaths(Class<?> routedClass) {
        Optional<RequestMapping> classRequestMapping = getAnnotationFromClass(routedClass, RequestMapping.class);
        return or(classRequestMapping.map(a -> getNonEmptyValue(a.value())),
                classRequestMapping.map(a -> getNonEmptyValue(a.path())))
                .orElse(DEFAULT_SET);
    }

    private void processRoutedMethod(Method routedMethod, ClassRoutesBuilder classRoutesBuilder) {
        Set<String> gatewayPathsFrom = getGatewayRequestMappingPaths(routedMethod);
        Set<String> facadePathsFrom = getFacadeGatewayRequestMappingPaths(routedMethod);
        Set<String> pathsTo = getRequestMappingPaths(routedMethod);

        classRoutesBuilder.withMethod(new MethodRoutesBuilder(microserviceName, routeHostMapping)
                .withGatewayPathsFrom(gatewayPathsFrom)
                .withFacadeGatewayPathsFrom(facadePathsFrom)
                .withPathsTo(pathsTo)
                .withRouteAnnotation(AnnotationUtils.findAnnotation(routedMethod, Routes.class))
                .withRouteAnnotation(AnnotationUtils.findAnnotation(routedMethod, Route.class))
                .withFacadeRouteAnnotation(AnnotationUtils.findAnnotation(routedMethod, FacadeRoute.class)));
    }

    private Set<String> getGatewayRequestMappingPaths(Method routedMethod) {
        Optional<GatewayRequestMapping> methodRequestMapping = getAnnotationFromMethod(routedMethod, GatewayRequestMapping.class);
        return or(methodRequestMapping.map(a -> getNonEmptyValue(a.value())),
                methodRequestMapping.map(a -> getNonEmptyValue(a.path())))
                .orElse(null);
    }

    private Set<String> getRequestMappingPaths(Method routedMethod) {
        Optional<RequestMapping> methodRequestMapping = getAnnotationFromMethod(routedMethod, RequestMapping.class);
        Optional<GetMapping> methodGetMapping = getAnnotationFromMethod(routedMethod, GetMapping.class);
        Optional<PostMapping> methodPostMapping = getAnnotationFromMethod(routedMethod, PostMapping.class);
        Optional<PutMapping> methodPutMapping = getAnnotationFromMethod(routedMethod, PutMapping.class);
        Optional<DeleteMapping> methodDeleteMapping = getAnnotationFromMethod(routedMethod, DeleteMapping.class);
        Optional<PatchMapping> methodPatchMapping = getAnnotationFromMethod(routedMethod, PatchMapping.class);

        return or(methodRequestMapping.map(a -> getNonEmptyValue(a.value())),
                methodRequestMapping.map(a -> getNonEmptyValue(a.path())))

                .orElse(or(methodGetMapping.map(a -> getNonEmptyValue(a.value())),
                        methodGetMapping.map(a -> getNonEmptyValue(a.path())))

                        .orElse(or(methodPostMapping.map(a -> getNonEmptyValue(a.value())),
                                methodPostMapping.map(a -> getNonEmptyValue(a.path())))

                                .orElse(or(methodPutMapping.map(a -> getNonEmptyValue(a.value())),
                                        methodPutMapping.map(a -> getNonEmptyValue(a.path())))

                                        .orElse(or(methodDeleteMapping.map(a -> getNonEmptyValue(a.value())),
                                                methodDeleteMapping.map(a -> getNonEmptyValue(a.path())))

                                                .orElse(or(methodPatchMapping.map(a -> getNonEmptyValue(a.value())),
                                                        methodPatchMapping.map(a -> getNonEmptyValue(a.path())))
                                                        .orElse(DEFAULT_SET))))
                        )
                );
    }

    private Set<String> getFacadeGatewayRequestMappingPaths(Class<?> routedClass) {
        Optional<FacadeGatewayRequestMapping> classRequestMapping = getAnnotationFromClass(routedClass, FacadeGatewayRequestMapping.class);
        return or(classRequestMapping.map(a -> getNonEmptyValue(a.value())),
                classRequestMapping.map(a -> getNonEmptyValue(a.path())))
                .orElse(null);
    }

    private Set<String> getFacadeGatewayRequestMappingPaths(Method routedMethod) {
        Optional<FacadeGatewayRequestMapping> methodRequestMapping = getAnnotationFromMethod(routedMethod, FacadeGatewayRequestMapping.class);
        return or(methodRequestMapping.map(a -> getNonEmptyValue(a.value())),
                methodRequestMapping.map(a -> getNonEmptyValue(a.path())))
                .orElse(null);
    }

    private Set<String> getNonEmptyValue(String[] originalValue) {
        return (null == originalValue || originalValue.length == 0) ? null
                : Arrays.stream(originalValue).map(routeFormatter::processRoute).collect(Collectors.toSet());
    }
}
