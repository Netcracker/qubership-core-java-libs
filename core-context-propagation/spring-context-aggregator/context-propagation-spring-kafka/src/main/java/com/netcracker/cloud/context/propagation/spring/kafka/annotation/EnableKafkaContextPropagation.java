package com.netcracker.cloud.context.propagation.spring.kafka.annotation;

import org.qubership.cloud.context.propagation.spring.kafka.KafkaContextPropagationConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({KafkaContextPropagationConfiguration.class})
public @interface EnableKafkaContextPropagation {
}
