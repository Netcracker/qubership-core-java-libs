package com.netcracker.cloud.podsecrets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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
        this.secrets = new CacheableValue<>(config.getTtl(), this::reloadSecrets);
    }

    private Map<String, String> reloadSecrets() {
        log.debug("Load secrets from: {}", config.getBaseDir());
        if (Files.isDirectory(config.getBaseDir())) {
            try {
                return Files.list(config.getBaseDir())
                        .peek(s -> log.debug("process: {}", s))
                        .filter(p -> !Files.isDirectory(p))
                        .map(path -> Map.entry(path.getFileName().toString(), loadSecretValue(path)))
                        .flatMap(this::variator)
                        .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue, (a, b) -> a));
            } catch (IOException e) {
                log.error("Error process secrets folder", e);
            }
        } else {
            log.debug("Secrets folder not found by: {}", config.getBaseDir());
        }
        return Map.of();
    }

    private Stream<Map.Entry<String, String>> variator(Map.Entry<String, String> e) {
        // now we should add key aliases, so user would find value by:
        // - `DB_PASSWORD`
        // - `db_password`
        // - `db.password`
        return Stream.of(
                Map.entry(e.getKey().toLowerCase(), e.getValue()),
                Map.entry(e.getKey().toUpperCase(), e.getValue()),
                Map.entry(e.getKey().toLowerCase().replaceAll("[^a-zA-Z0-9]", "."), e.getValue())
        );
    }

    private String loadSecretValue(Path path) {
        try {
            log.debug("Load secret data from: {}", path);
            return Files.readString(path);
        } catch (IOException e) {
            log.error("Error load secret value from: {}", path, e);
            return null;
        }
    }

    public Map<String, String> getSecrets() {
        return secrets.get();
    }
}
