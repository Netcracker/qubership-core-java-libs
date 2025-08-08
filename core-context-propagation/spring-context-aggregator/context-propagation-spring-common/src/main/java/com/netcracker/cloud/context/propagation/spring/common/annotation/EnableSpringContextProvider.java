package com.netcracker.cloud.context.propagation.spring.common.annotation;

import com.netcracker.cloud.context.propagation.spring.common.configuration.SpringContextProviderConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(SpringContextProviderConfiguration.class)
public @interface EnableSpringContextProvider {
}
