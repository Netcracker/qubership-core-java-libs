package com.netcracker.cloud.podsecrets.spring;

import com.netcracker.cloud.podsecrets.PodSecretsLoader;
import org.springframework.core.env.EnumerablePropertySource;

import java.util.stream.Stream;

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
        // because this method returns plain array and we cannot support case-insensitive matching,
        // just duplicate all properties in lower-case and upper case versions
        return source.getSecrets().keySet().stream()
                .flatMap(key -> Stream.of(key.toLowerCase(), key.toUpperCase()))
                .toArray(String[]::new);
    }

    @Override
    public Object getProperty(String name) {
        return source.getSecrets().get(name);
    }
}
