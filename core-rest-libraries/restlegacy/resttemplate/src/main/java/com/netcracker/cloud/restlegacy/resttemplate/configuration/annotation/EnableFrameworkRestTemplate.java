package com.netcracker.cloud.restlegacy.resttemplate.configuration.annotation;

import com.netcracker.cloud.restlegacy.resttemplate.configuration.RestTemplateConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(RestTemplateConfiguration.class)
public @interface EnableFrameworkRestTemplate {
}
