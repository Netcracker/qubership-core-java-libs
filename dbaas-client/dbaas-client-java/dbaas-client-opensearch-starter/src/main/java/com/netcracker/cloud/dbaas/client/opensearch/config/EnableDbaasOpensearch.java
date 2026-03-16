package com.netcracker.cloud.dbaas.client.opensearch.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@EnableServiceDbaasOpensearch
@EnableTenantDbaasOpensearch
public @interface EnableDbaasOpensearch {
}
