package org.qubership.cloud.dbaas.client.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@EnableServiceDbaasClickhouse
@EnableTenantDbaasClickhouse
public @interface EnableDbaasClickhouse {
}
