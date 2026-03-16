package com.netcracker.cloud.microserviceframework.config.annotation;

import com.netcracker.cloud.microserviceframework.config.MongoEvolutionConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({MongoEvolutionConfiguration.class})
public @interface EnableMongoEvolutionPostProcessor {
}
