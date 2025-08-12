package com.netcracker.cloud.routesregistration.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.netcracker.cloud.routesregistration.common.gateway.route.Constants.UNSPECIFIED_TIMEOUT_FOR_ROUTE;


@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface FacadeRoute {

    /**
     * @return {@code long} value equal to amount of milliseconds or -1 if special timeout is missing.
     * This timeout specifies the timeout for the request on this route.
     * If timeout is missing, the default value will be set according to gateway configuration
     */
    long timeout() default UNSPECIFIED_TIMEOUT_FOR_ROUTE;

    String[] gateways() default "";
}
