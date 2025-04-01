package org.qubership.cloud.microserviceframework.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableCustomChangeLogsPackages {
    String tenantAwareChangeLogsPackage() default "";
    String microserviceChangeLogsPackage() default "";
}

