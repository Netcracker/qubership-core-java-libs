package org.qubership.cloud.frameworkextensions.swagger.config.annotation;

import org.qubership.cloud.frameworkextensions.swagger.config.SwaggerAuthConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import(SwaggerAuthConfiguration.class)
public @interface EnableSwaggerAuth {
}
