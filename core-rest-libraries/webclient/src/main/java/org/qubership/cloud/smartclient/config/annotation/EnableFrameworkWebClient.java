package org.qubership.cloud.smartclient.config.annotation;

import org.qubership.cloud.smartclient.rest.webclient.config.WebClientConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(WebClientConfiguration.class)
public @interface EnableFrameworkWebClient {
}