package org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(TestcontainersExecutionCondition.class)
public @interface SkipOnDemand { }
