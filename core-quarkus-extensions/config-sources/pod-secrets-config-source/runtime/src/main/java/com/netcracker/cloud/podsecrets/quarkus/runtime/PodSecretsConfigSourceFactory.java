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

/**
 * SmallRye {@link ConfigSourceFactory} that creates a {@link PodSecretsConfigSource}
 * when the secrets directory is available.
 */
public class PodSecretsConfigSourceFactory implements ConfigSourceFactory {

    private static final Logger log = Logger.getLogger(PodSecretsConfigSourceFactory.class);

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
        String enabled = getValue(context, "pod.secrets.enabled", "true");
        if (!"true".equalsIgnoreCase(enabled)) {
            log.debug("Pod-secrets config source is disabled");
            return Collections.emptyList();
        }

        PodSecretsLoaderConfig config = fromContext(context);
        PodSecretsLoader loader = new PodSecretsLoader(config);

        if (!loader.isAvailable()) {
            log.debugf("Pod-secrets directory not available: %s", config.getBaseDir());
            return Collections.emptyList();
        }

        return Collections.singletonList(new PodSecretsConfigSource(loader));
    }

    static PodSecretsLoaderConfig fromContext(ConfigSourceContext context) {
        String dirStr = getValue(context, "pod.secrets.dir",
                Optional.ofNullable(System.getenv("POD_SECRETS_DIR"))
                        .orElse(PodSecretsLoaderConfig.DEFAULT_BASE_DIR.toString()));
        Path dir = Paths.get(dirStr);

        String ttlStr = getValue(context, "pod.secrets.ttl", null);
        Duration ttl = ttlStr != null ? Duration.parse(ttlStr) : PodSecretsLoaderConfig.DEFAULT_TTL;

        return PodSecretsLoaderConfig.of(dir, ttl);
    }

    private static String getValue(ConfigSourceContext context, String key, String defaultValue) {
        io.smallrye.config.ConfigValue cv = context.getValue(key);
        if (cv != null && cv.getValue() != null && !cv.getValue().isBlank()) {
            return cv.getValue();
        }
        return defaultValue;
    }
}
