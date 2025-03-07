package org.qubership.cloud.frameworkextensions.metrics.config.annotation;

import org.qubership.cloud.frameworkextensions.metrics.config.CustomDelegatingMetricsConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({CustomDelegatingMetricsConfiguration.class})
public @interface EnableCustomMetrics {
}