package com.netcracker.cloud.restlegacy.restclient.configuration.annotation;

import com.netcracker.cloud.restlegacy.restclient.error.v2.ControllersAdvice;
import com.netcracker.cloud.restlegacy.restclient.error.v2.ExceptionHandlingV2MainConfiguration;
import com.netcracker.cloud.restlegacy.restclient.error.v2.ExceptionHandlingV2_0Configuration;
import com.netcracker.cloud.restlegacy.restclient.error.v2.v2_1.ExceptionHandlingV2_1Configuration;
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
