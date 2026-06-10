package com.netcracker.cloud.podsecrets;

import lombok.Value;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;

/**
 * Immutable configuration for {@link PodSecretsLoader}.
 */
@Value
public final class PodSecretsLoaderConfig {
    public static final Path DEFAULT_BASE_DIR = Paths.get("/etc/secrets/pod-secrets/");
    public static final Duration DEFAULT_TTL = Duration.ofSeconds(60);

    private final Path baseDir;
    private final Duration ttl;

    public static PodSecretsLoaderConfig of(Path baseDir, Duration ttl) {
        return new PodSecretsLoaderConfig(baseDir, ttl);
    }

    /**
     * Reads configuration from system properties / env variables:
     * <ul>
     *   <li>{@code pod.secrets.dir} (system property) or {@code POD_SECRETS_DIR} (env)</li>
     *   <li>{@code pod.secrets.ttl} — ISO-8601 duration, e.g. {@code PT30S}</li>
     * </ul>
     */
    public static PodSecretsLoaderConfig fromSystem() {
        Path dir = Optional.ofNullable(System.getProperty("pod.secrets.dir"))
                .map(Paths::get)
                .orElseGet(() -> Optional.ofNullable(System.getenv("POD_SECRETS_DIR"))
                        .map(Paths::get)
                        .orElse(DEFAULT_BASE_DIR));

        Duration ttl = Optional.ofNullable(System.getProperty("pod.secrets.ttl"))
                .map(Duration::parse)
                .orElse(DEFAULT_TTL);

        return new PodSecretsLoaderConfig(dir, ttl);
    }
}
