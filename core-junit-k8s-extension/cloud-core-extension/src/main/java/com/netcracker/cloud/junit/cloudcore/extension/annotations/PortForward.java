package com.netcracker.cloud.junit.cloudcore.extension.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface PortForward {

    Value serviceName();

    IntValue port() default @IntValue(8080);

    // http, https, ftp etc. (applicable only to URI/URL injections)
    Value protocol() default @Value("http");

    Cloud cloud() default @Cloud();

}
