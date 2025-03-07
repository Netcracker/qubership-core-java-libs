package org.qubership.cloud.restlegacy.restclient.configuration.annotation;

import org.qubership.cloud.restlegacy.restclient.configuration.DefaultRetryTemplateConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({DefaultRetryTemplateConfiguration.class})
public @interface EnableDefaultRetryTemplate {
}