package org.qubership.cloud.mongoevolution.java.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for Class method.
 * Set of changes to be added to the DB. Many changesets are included in one changelog.
 * @see ChangeLog
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChangeSet {

    /**
     * Integer number that provide an order of ChangeSet appliance.
     * If not set, default is 0.
     * @return order of ChangeSet
     */
    int order() default 0;
}

