package com.netcracker.cloud.podsecrets.quarkus.runtime;

import com.netcracker.cloud.podsecrets.PodSecretsLoader;
import com.netcracker.cloud.podsecrets.PodSecretsLoaderConfig;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

/**
 * SmallRye {@link ConfigSourceFactory} that creates a {@link PodSecretsConfigSource}
 * when the secrets directory is available.
 */
public class PodSecretsConfigSourceFactory implements ConfigSourceFactory {
    private static final Logger log = Logger.getLogger(PodSecretsConfigSourceFactory.class);
    private static final PodSecretsLoaderConfig DEFAULT_CONFIG = PodSecretsLoaderConfig.fromSystem();

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
        boolean enabled = getValue(context, "pod.secrets.enabled", true, Boolean::valueOf);
        if (!enabled) {
            log.debug("Pod-secrets config source is disabled");
            return Collections.emptyList();
        }

        PodSecretsLoaderConfig config = fromContext(context);
        log.debugf("Pod-secrets config: %s", config);
        PodSecretsLoader loader = new PodSecretsLoader(config);

        if (!loader.isAvailable()) {
            log.debugf("Pod-secrets directory not available: %s", config.getBaseDir());
            return Collections.emptyList();
        }

        return Collections.singletonList(new PodSecretsConfigSource(loader));
    }

    static PodSecretsLoaderConfig fromContext(ConfigSourceContext context) {
        var dir = getValue(context, "pod.secrets.dir", DEFAULT_CONFIG.getBaseDir(), Paths::get);
        var ttl = getValue(context, "pod.secrets.ttl", PodSecretsLoaderConfig.DEFAULT_TTL, Duration::parse);
        return PodSecretsLoaderConfig.of(dir, ttl);
    }

    private static <T> T getValue(ConfigSourceContext context, String key, T defaultValue, Function<String, T> converter) {
        io.smallrye.config.ConfigValue cv = context.getValue(key);
        if (cv != null && cv.getValue() != null && !cv.getValue().isBlank()) {
            return converter.apply(cv.getValue());
        }
        return defaultValue;
    }
}
