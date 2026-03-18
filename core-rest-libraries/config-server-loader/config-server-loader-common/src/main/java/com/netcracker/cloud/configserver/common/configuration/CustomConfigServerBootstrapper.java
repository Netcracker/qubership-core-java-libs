package com.netcracker.cloud.configserver.common.configuration;

import lombok.AllArgsConstructor;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistryInitializer;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.cloud.config.client.ConfigServerBootstrapper;

public class CustomConfigServerBootstrapper implements BootstrapRegistryInitializer {

    public CustomConfigServerBootstrapper() {
    }

    @Override
    public void initialize(BootstrapRegistry registry) {
        registry.register(ConfigServerBootstrapper.LoaderInterceptor.class,
                context -> new CustomLoaderInterceptor(new CustomConfigServerDataLoader()));
    }

    @AllArgsConstructor
    static final class CustomLoaderInterceptor implements ConfigServerBootstrapper.LoaderInterceptor {
        private final CustomConfigServerDataLoader customConfigServerDataLoader;

        @Override
        public ConfigData apply(ConfigServerBootstrapper.LoadContext loadContext) {
            return customConfigServerDataLoader.doLoad(loadContext.getLoaderContext(), loadContext.getResource());
        }
    }
}
