package org.qubership.cloud.routesregistration.common.spring.gateway.route.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;

/**
 * Annotation for Class or Method to mark out microservice custom path for facade gateway routing.
 * Based on {@link org.springframework.web.bind.annotation.RequestMapping} annotation idea.
 * <p>
 * Possible annotations:
 * &#64;FacadeGatewayRequestMapping({@link String[]})
 * &#64;FacadeGatewayRequestMapping(value = {@link String[]})
 * &#64;FacadeGatewayRequestMapping(path = {@link String[]})
 * </p>
 *
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface FacadeGatewayRequestMapping {
    @AliasFor("path")
    String[] value() default {};

    /**
     * @return {@link String[]} custom path to the resource 'pathFrom' for routing: pathFrom -&gt; microservice-url/pathTo
     * If not specified, pathFrom is equal to pathTo &amp; retrieved from RequestMapping annotation.
     */
    @AliasFor("value")
    String[] path() default {};
}
