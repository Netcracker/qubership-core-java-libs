package org.qubership.cloud.routesregistration.common.spring.gateway.route;


import org.qubership.cloud.routesregistration.common.annotation.FacadeRoute;
import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.qubership.cloud.routesregistration.common.annotation.Routes;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import static org.qubership.cloud.routesregistration.common.gateway.route.Constants.UNSPECIFIED_TIMEOUT_FOR_ROUTE;

public class RouteAnnotationProcessorUtil {

    private static final RouteType DEFAULT_ROUTE_TYPE = RouteType.INTERNAL;

    public static boolean isAnnotatedWithRoute(Method method) {
        return getAnnotationFromMethod(method, Route.class).isPresent()
                || getAnnotationFromMethod(method, Routes.class).isPresent();
    }

    public static boolean isAnnotatedWithFacadeRoute(Method method) {
        return getAnnotationFromMethod(method, FacadeRoute.class).isPresent();
    }

    public static boolean isAnnotatedWithFacadeRoute(Class<?> clazz) {
        return getAnnotationFromClass(clazz, FacadeRoute.class).isPresent()
                || Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz)).anyMatch(RouteAnnotationProcessorUtil::isAnnotatedWithFacadeRoute);
    }

    public static boolean isAnnotatedWithRoute(Class<?> clazz) {
        return getAnnotationFromClass(clazz, Route.class).isPresent()
                || getAnnotationFromClass(clazz, Routes.class).isPresent()
                || Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clazz)).anyMatch(RouteAnnotationProcessorUtil::isAnnotatedWithRoute);
    }

    public static <T extends Annotation> Optional<T> getAnnotationFromClass(Class<?> clazz, Class<T> annotation) {
        return Optional.ofNullable(AnnotationUtils.findAnnotation(clazz, annotation));
    }

    public static <T extends Annotation> Optional<T> getAnnotationFromMethod(Method method, Class<T> annotation) {
        return Optional.ofNullable(AnnotationUtils.findAnnotation(method, annotation));
    }

    public static RouteType getAnnotationRouteType(Route route) {
        return (route.value() == DEFAULT_ROUTE_TYPE) ? route.type() : route.value();
    }

    public static Long getAnnotationRouteTimeout(Route route) {
        return (route.timeout() == UNSPECIFIED_TIMEOUT_FOR_ROUTE) ? null : route.timeout();
    }

    public static Long getAnnotationRouteTimeout(FacadeRoute route) {
        return (route.timeout() == UNSPECIFIED_TIMEOUT_FOR_ROUTE) ? null : route.timeout();
    }

    static <T> Optional<T> or(Optional<T> first, Optional<T> second) {
        return first.isPresent() ? first : second;
    }

}
