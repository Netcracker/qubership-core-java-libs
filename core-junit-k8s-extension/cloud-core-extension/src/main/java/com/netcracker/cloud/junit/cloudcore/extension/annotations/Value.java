package com.netcracker.cloud.junit.cloudcore.extension.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Value {
    /**
     * Specify value if you want the as-is value to be resolved by this annotation
     *
     * @return value as is
     */
    String value() default "";

    /**
     * Specify property if you want the value to be resolved by System.getProperty(property()) method.
     *
     * @return property name to resolve the value from
     */
    String prop() default "";

}
