package com.netcracker.cloud.podsecrets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PodSecretsLoader {
    private static final Logger log = LoggerFactory.getLogger(PodSecretsLoader.class);

    private final PodSecretsLoaderConfig config;
    private final CacheableValue<Map<String, String>> secrets;

    public PodSecretsLoader(PodSecretsLoaderConfig config) {
        this.config = config;
        this.secrets = new CacheableValue<>(config.getTtl(), this::reloadSecrets, Map.of());
    }

    private Map<String, String> reloadSecrets() {
        log.debug("Load secrets from: {}", config.getBaseDir());
        if (!Files.isDirectory(config.getBaseDir())) {
            log.debug("Secrets folder not found by: {}", config.getBaseDir());
            return Map.of();
        }

        Map<String, String> result;
        try (Stream<Path> stream = Files.list(config.getBaseDir())) {
            result = stream
                    .filter(p -> !Files.isDirectory(p))
                    .peek(s -> log.debug("process: {}", s))
                    .flatMap(path -> loadSecretValue(path)
                            .map(value -> Map.entry(path.getFileName().toString(), value))
                            .stream())
                    .flatMap(this::variator)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
        } catch (IOException e) {
            log.error("Cannot list pod-secrets directory {}: {}", config.getBaseDir(), e.getMessage());
            throw new UncheckedIOException(e);
        }

        Map<String, String> merged = new HashMap<>(result);
        secrets.getCurrent().forEach((key, value) -> merged.putIfAbsent(key, value));

        log.debug("Pod-secrets key names: {} (dir={})", result.keySet(), config.getBaseDir());
        return Map.copyOf(merged);
    }

    private Stream<Map.Entry<String, String>> variator(Map.Entry<String, String> e) {
        // add key aliases so user can look up value by:
        // - `DB_PASSWORD`
        // - `db_password`
        // - `db.password`
        return Stream.of(
                Map.entry(e.getKey().toLowerCase(), e.getValue()),
                Map.entry(e.getKey().toUpperCase(), e.getValue()),
                Map.entry(e.getKey().toLowerCase().replaceAll("[^a-z0-9]", "."), e.getValue())
        );
    }

    private Optional<String> loadSecretValue(Path path) {
        try {
            log.debug("Load secret data from: {}", path);
            return Optional.of(Files.readString(path));
        } catch (IOException e) {
            log.warn("Cannot read pod-secret file {}: {}", path.getFileName(), e.getMessage());
            return Optional.empty();
        }
    }

    public Map<String, String> getSecrets() {
        return secrets.get();
    }
}
