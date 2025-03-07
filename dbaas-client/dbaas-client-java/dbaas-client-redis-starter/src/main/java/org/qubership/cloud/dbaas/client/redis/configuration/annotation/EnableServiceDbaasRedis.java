package org.qubership.cloud.dbaas.client.redis.configuration.annotation;

import org.qubership.cloud.dbaas.client.redis.configuration.ServiceDbaasRedisConfiguration;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import(ServiceDbaasRedisConfiguration.class)
public @interface EnableServiceDbaasRedis {
}
