package org.qubership.cloud.maas.client.spring.rabbit.annotation;

import org.qubership.cloud.maas.spring.localdev.MaasLocalDevConfig;
import org.qubership.cloud.maas.client.spring.rabbit.DynamicQueueBindingManagerConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({DynamicQueueBindingManagerConfiguration.class, MaasLocalDevConfig.class})
public @interface EnableDynamicQueueBindings {
}
