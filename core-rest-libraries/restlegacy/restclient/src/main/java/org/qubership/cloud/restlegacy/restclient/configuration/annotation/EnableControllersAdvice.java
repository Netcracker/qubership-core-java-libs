package org.qubership.cloud.restlegacy.restclient.configuration.annotation;

import org.qubership.cloud.restlegacy.restclient.error.v2.ControllersAdvice;
import org.qubership.cloud.restlegacy.restclient.error.v2.ExceptionHandlingV2MainConfiguration;
import org.qubership.cloud.restlegacy.restclient.error.v2.ExceptionHandlingV2_0Configuration;
import org.qubership.cloud.restlegacy.restclient.error.v2.v2_1.ExceptionHandlingV2_1Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({ControllersAdvice.class, ExceptionHandlingV2MainConfiguration.class,
        ExceptionHandlingV2_0Configuration.class, ExceptionHandlingV2_1Configuration.class})
public @interface EnableControllersAdvice {
}