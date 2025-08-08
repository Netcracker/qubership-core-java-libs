package com.netcracker.cloud.microserviceframework.config.annotation;

import com.netcracker.cloud.microserviceframework.config.MSInfoProviderConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({MSInfoProviderConfiguration.class})
public @interface EnableMSInfoProvider {
}
