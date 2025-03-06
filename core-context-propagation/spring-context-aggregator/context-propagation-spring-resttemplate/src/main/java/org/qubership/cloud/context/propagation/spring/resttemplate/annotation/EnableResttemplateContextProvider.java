package org.qubership.cloud.context.propagation.spring.resttemplate.annotation;

import org.qubership.cloud.context.propagation.spring.common.annotation.EnableSpringContextProvider;
import org.qubership.cloud.context.propagation.spring.resttemplate.configuration.ResttemplateContextProviderConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@EnableSpringContextProvider
@Import(ResttemplateContextProviderConfiguration.class)
public @interface EnableResttemplateContextProvider {
}
