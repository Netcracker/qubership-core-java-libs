package com.netcracker.cloud.junit.cloudcore.extension.provider;

import com.netcracker.cloud.junit.cloudcore.extension.callbacks.CloudCoreJunitCallback;

import java.lang.reflect.Field;
import java.util.ServiceLoader;
import java.util.function.Predicate;

/**
 * Provides an instance for the field of a test instance.
 *  {@link CloudCoreJunitCallback } listens for beforeAll and afterAll lifecycle events.
 *  all fields of a testInstance can be injected with an instance provided by this interface.
 *  FieldInstanceProvider are loaded by {@link ServiceLoader}
 *  available providers are filtered by {@link Predicate<Field>#test(Field)} method
 *  the provider with min order is called
 */
public interface FieldInstanceProvider extends Predicate<Field> {

    /**
     * Order of the provider. Lower order providers are called first.
     * @return
     */
    default int order() {
        return Integer.MAX_VALUE;
    }

    /**
     * Create an instance for the field's type
     *
     * @param testInstance test instance to create instance for
     * @param field        field to create instance for
     * @return created instance
     * @throws Exception
     */
    Object createInstance(Object testInstance, Field field) throws Exception;

    /**
     * Destroy the instance created by the createInstance method
     *
     * @param testInstance test instance to destroy instance for
     * @param field        field to destroy instance for
     * @throws Exception
     */
    void destroyInstance(Object testInstance, Field field) throws Exception;

}
