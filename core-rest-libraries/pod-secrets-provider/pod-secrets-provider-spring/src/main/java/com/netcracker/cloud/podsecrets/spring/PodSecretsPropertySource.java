package com.netcracker.cloud.podsecrets.spring;

import com.netcracker.cloud.podsecrets.PodSecretsLoader;
import org.springframework.core.env.EnumerablePropertySource;

/**
 * Spring {@link EnumerablePropertySource} backed by {@link PodSecretsLoader}.
 *
 * <p>Keys are exposed in all three canonical forms (lowercase, UPPER_CASE, dot.notation)
 * so that Spring's relaxed-binding mechanism can resolve them regardless of the notation used
 * by the consumer.
 */
public class PodSecretsPropertySource extends EnumerablePropertySource<PodSecretsLoader> {

    public static final String SOURCE_NAME = "pod-secrets-property-source";

    public PodSecretsPropertySource(PodSecretsLoader loader) {
        super(SOURCE_NAME, loader);
    }

    @Override
    public String[] getPropertyNames() {
        return source.propertyNames().toArray(new String[0]);
    }

    @Override
    public Object getProperty(String name) {
        return source.getValue(name);
    }
}
