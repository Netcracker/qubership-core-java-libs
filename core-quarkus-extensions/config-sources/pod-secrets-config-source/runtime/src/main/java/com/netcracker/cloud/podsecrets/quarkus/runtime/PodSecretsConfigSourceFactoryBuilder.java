package com.netcracker.cloud.podsecrets.quarkus.runtime;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

import static com.netcracker.cloud.podsecrets.quarkus.runtime.PodSecretsConfigSource.PRIORITY;

/**
 * {@link ConfigBuilder} registered via {@code RunTimeConfigBuilderBuildItem} in the deployment module.
 * Wires the {@link PodSecretsConfigSourceFactory} into SmallRye config with no action required
 * from the consuming application.
 */
public class PodSecretsConfigSourceFactoryBuilder implements ConfigBuilder {

    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        return builder.withSources(new PodSecretsConfigSourceFactory());
    }

    @Override
    public int priority() {
        return PRIORITY;
    }
}
