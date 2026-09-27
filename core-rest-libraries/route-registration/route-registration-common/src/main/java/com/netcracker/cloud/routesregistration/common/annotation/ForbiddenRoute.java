package com.netcracker.cloud.routesregistration.common.annotation;

import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for Class or Method to mark out its gateway path as forbidden on the listed border gateways.
 * <p>
 * Used only by the Istio HTTPRoute generator maven plugin, which turns it into an Istio {@code AuthorizationPolicy}
 * DENY rule. Legacy runtime route registration ignores this annotation.
 * <p>
 * A class-level annotation forbids only the class-level gateway path(s), not each method path separately.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ForbiddenRoute {

    /**
     * Border gateways on which the gateway path is forbidden. Each value names a single gateway and does not imply
     * the wider gateways: {@link RouteType#PUBLIC} is the public gateway, {@link RouteType#PRIVATE} is the private
     * gateway and {@link RouteType#INTERNAL} is the internal gateway. {@link RouteType#FACADE} is not supported.
     *
     * @return border gateways on which the gateway path is forbidden
     */
    RouteType[] value();
}
