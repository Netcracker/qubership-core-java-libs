package org.qubership.cloud.consul.config.source.runtime;

import org.qubership.cloud.consul.provider.common.TokenStorage;
import org.qubership.cloud.quarkus.consul.client.ConsulClient;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;
import jakarta.enterprise.inject.spi.CDI;

import static org.qubership.cloud.consul.config.source.runtime.ConsulConfigSource.PRIORITY;

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
