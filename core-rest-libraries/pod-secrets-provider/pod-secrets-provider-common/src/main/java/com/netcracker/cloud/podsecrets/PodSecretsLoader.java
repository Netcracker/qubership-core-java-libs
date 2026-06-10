package com.netcracker.cloud.podsecrets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

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
                        .collect(Collectors.toMap(
                                        path -> path.getFileName().toString(),
                                        path -> loadSecretValue(path),
                                        (a, b) -> a,
                                        () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
                                )
                        );
            } catch (IOException e) {
                log.error("Error process secrets folder", e);
            }
        } else {
            log.debug("Secrets folder not found by: {}", config.getBaseDir());
        }
        return Map.of();
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
