package com.netcracker.cloud.podsecrets.spring;

import com.netcracker.cloud.podsecrets.PodSecretsLoader;
import com.netcracker.cloud.podsecrets.PodSecretsLoaderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

/**
 * Registers {@link PodSecretsPropertySource} in the Spring {@link ConfigurableEnvironment}
 * before {@code systemEnvironment} so that secrets from the pod-secrets directory
 * override environment variables.
 *
 * <p>Registration happens at {@code ConfigDataEnvironmentPostProcessor.ORDER - 1} so that
 * the source is available during {@code spring.config.import} resolution.
 *
 * <p>If the secrets directory is absent or empty the processor exits silently —
 * the application continues to resolve properties from env-vars as before.
 */
public class PodSecretsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(PodSecretsEnvironmentPostProcessor.class);

    public static final int ORDER = ConfigDataEnvironmentPostProcessor.ORDER - 1;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String enabled = environment.getProperty("pod.secrets.enabled", "true");
        if (!"true".equalsIgnoreCase(enabled)) {
            log.debug("Pod-secrets property source is disabled (pod.secrets.enabled={})", enabled);
            return;
        }

        PodSecretsLoaderConfig config = PodSecretsLoaderConfig.fromSystem();
        PodSecretsLoader loader = new PodSecretsLoader(config);

        PodSecretsPropertySource propertySource = new PodSecretsPropertySource(loader);
        environment.getPropertySources().addBefore(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                propertySource);

        log.info("Pod-secrets property source registered before systemEnvironment (dir={})", config.getBaseDir());
    }
}
