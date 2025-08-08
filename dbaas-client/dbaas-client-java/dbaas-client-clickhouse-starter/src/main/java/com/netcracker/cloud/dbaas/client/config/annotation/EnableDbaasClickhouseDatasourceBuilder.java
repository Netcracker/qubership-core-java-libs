package com.netcracker.cloud.dbaas.client.config.annotation;

import com.netcracker.cloud.dbaas.client.config.DbaasClickhouseConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import(DbaasClickhouseConfiguration.class)
public @interface EnableDbaasClickhouseDatasourceBuilder {
}
