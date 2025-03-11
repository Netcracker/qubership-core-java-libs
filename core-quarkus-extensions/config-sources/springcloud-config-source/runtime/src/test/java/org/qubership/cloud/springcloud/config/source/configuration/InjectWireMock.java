package org.qubership.cloud.springcloud.config.source.configuration;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectWireMock {

}
