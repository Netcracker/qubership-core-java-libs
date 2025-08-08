package com.netcracker.cloud.context.propagation.spring.rabbit.annotation;

import com.netcracker.cloud.context.propagation.spring.rabbit.RabbitContextPropagationConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(RabbitContextPropagationConfiguration.class)
public @interface EnableRabbitContextPropagation {
}
