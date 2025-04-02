package org.qubership.cloud.junit.cloudcore.extension.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface IntValue {
    /**
     * Specify value if you want the as-is value to be resolved by this annotation
     *
     * @return value as is
     */
    int value() default -1;

    /**
     * Specify property if you want the value to be resolved by System.getProperty(property()) method.
     *
     * @return property name to resolve the value from
     */
    String prop() default "";

}
