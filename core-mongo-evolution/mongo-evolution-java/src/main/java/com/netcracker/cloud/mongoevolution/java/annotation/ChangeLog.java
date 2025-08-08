package org.qubership.cloud.mongoevolution.java.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation for Class.
 * Class containing particular changesets (@{@link ChangeSet})
 *
 * @see ChangeSet
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChangeLog {

    /**
     * Long number that provide a version for changelog classes.
     * Required field.
     *
     * @return version
     */
    long version();

    /**
     * Int number that provide an order of ChangeLog appliance.
     * If not set, default is 0.
     *
     * @return order of ChangeLog
     */
    int order() default 0;

    /**
     * Database classifier (name).
     * ChangeLog annotation should be applied to the database with this dbClassifier.
     *
     * @return dbClassifier
     */
    String dbClassifier() default "default";
}

