package com.netcracker.cloud.podsecrets.spring;

import com.netcracker.cloud.podsecrets.PodSecretsLoader;
import com.netcracker.cloud.podsecrets.PodSecretsLoaderConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Publishes a {@link PodSecretsLoader} bean for optional DI access from application code.
 *
 * <p>The bean is guarded by {@code pod.secrets.enabled} (default: {@code true}).
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "pod.secrets", name = "enabled", matchIfMissing = true)
public class PodSecretsAutoConfiguration {

    @Bean
    public PodSecretsLoader podSecretsLoader() {
        return new PodSecretsLoader(PodSecretsLoaderConfig.fromSystem());
    }
}
