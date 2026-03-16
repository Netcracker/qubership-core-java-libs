package com.netcracker.cloud.routesregistration.common.annotation;

import com.netcracker.cloud.routesregistration.common.gateway.route.Constants;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;

import java.lang.annotation.*;


/**
 * Annotation for Class or Method to mark out microservice available routes.
 * <p>
 * Possible annotations:
 * <p>
 * Route
 * means {@link Route#value} default value is used
 * <p>
 * Route(type = {@link RouteType})
 * means {@link Route#value} is used
 *
 * </p>
 */
@Repeatable(Routes.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Route {

    /**
     * <p>Route type corresponding to API gateway type.
     *
     * <p>If not specified, {@link Route#gateways()} will be used to decide to which gateways this route should be sent.
     *
     * <p>If both {@link Route#type()} and {@link Route#gateways()} are empty then {@link RouteType#INTERNAL} will be picked as a default.
     *
     * @return {@link RouteType} type corresponding to API gateway type
     */
    RouteType value() default RouteType.INTERNAL;

    /**
     * @return {@code long} value equal to amount of milliseconds or -1 if special timeout is missing.
     * This timeout specifies the timeout for the request on this route.
     * If timeout is missing, the default value will be set according to gateway configuration
     */
    long timeout() default Constants.UNSPECIFIED_TIMEOUT_FOR_ROUTE;

    /**
     * <p>Route type corresponding to API gateway type.
     *
     * <p>If not specified, {@link Route#gateways()} will be used to decide to which gateways this route should be sent.
     *
     * <p>If both {@link Route#type()} and {@link Route#gateways()} are empty then {@link RouteType#INTERNAL} will be picked as a default.
     *
     * @return {@link RouteType} type corresponding to API gateway type
     */
    RouteType type() default RouteType.INTERNAL;

    String[] gateways() default "";

    String[] hosts() default "";
}

