package com.netcracker.cloud.consul.config.source.runtime;

import com.netcracker.cloud.consul.provider.common.TokenStorage;
import com.netcracker.cloud.quarkus.consul.client.ConsulClient;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;
import jakarta.enterprise.inject.spi.CDI;

import static com.netcracker.cloud.consul.config.source.runtime.ConsulConfigSource.PRIORITY;

public class ConsulConfigSourceFactoryBuilder implements ConfigBuilder {

    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        return builder.withValidateUnknown(false).withSources(new ConsulConfigSourceFactory(
                CDI.current().select(ConsulClient.class).get(),
                CDI.current().select(TokenStorage.class).get()
        ));
    }

    @Override
    public int priority() {
        return PRIORITY;
    }
}
