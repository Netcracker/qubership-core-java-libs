package com.netcracker.cloud.podsecrets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Reads Kubernetes pod-secrets from a mounted directory and exposes them as
 * a property map with TTL-based caching.
 *
 * <p>Each file in the directory is treated as a secret:
 * file name = key (lowercase), file content = value (trailing newline stripped).
 *
 * <p>Keys are published in three forms to support relaxed binding:
 * {@code db_password}, {@code DB_PASSWORD}, {@code db.password}.
 */
public class PodSecretsLoader {

    private static final Logger log = LoggerFactory.getLogger(PodSecretsLoader.class);

    private final PodSecretsLoaderConfig config;
    /** Map key = normalised lowercase_underscore name, value = secret value. */
    private final CacheableValue<Map<String, String>> cache;

    public PodSecretsLoader(PodSecretsLoaderConfig config) {
        this.config = config;
        this.cache = new CacheableValue<>(config.getTtl(), this::load);
    }

    /**
     * Returns a snapshot with keys expanded to all three canonical forms.
     */
    public Map<String, String> snapshot() {
        return cache.get();
    }

    /**
     * Returns the value for the given property name, or {@code null} if absent.
     * The name is normalised before lookup (case-insensitive, dots == underscores).
     */
    public String getValue(String key) {
        String normalised = KeyExpander.normalise(key);
        return cache.get().get(normalised);
    }

    /**
     * Returns all property names in canonical forms.
     */
    public Set<String> propertyNames() {
        return cache.get().keySet();
    }

    /**
     * Returns {@code true} if the cache snapshot contains at least one key.
     * Uses the cached snapshot — no extra FS read on startup.
     */
    public boolean isAvailable() {
        return !cache.get().isEmpty();
    }

    private Map<String, String> load() {
        Path dir = config.getBaseDir();
        if (!Files.isDirectory(dir)) {
            log.debug("Pod-secrets directory does not exist: {}", dir);
            return Collections.emptyMap();
        }

        // File.listFiles() releases the directory handle immediately (avoids
        // Windows race where DirectoryStream handle outlives try-with-resources).
        File[] entries = dir.toFile().listFiles();
        if (entries == null) {
            log.debug("Error listing secrets directory {}: listFiles() returned null", dir);
            return Collections.emptyMap();
        }

        Map<String, String> raw = new HashMap<>();
        for (File entry : entries) {
            if (!entry.isDirectory()) {
                String fileName = entry.getName().toLowerCase();
                String value = readFile(entry.toPath());
                if (value != null) {
                    raw.put(fileName, value);
                }
            }
        }

        Map<String, String> expanded = new HashMap<>(raw.size() * 3);
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            for (String alias : KeyExpander.expand(entry.getKey())) {
                expanded.put(alias, entry.getValue());
            }
            expanded.put(entry.getKey(), entry.getValue());
        }

        log.info("Pod-secrets loaded {} key(s) from {}", raw.size(), dir);
        if (log.isDebugEnabled()) {
            log.debug("Pod-secrets key names: {}", raw.keySet());
        }
        return Collections.unmodifiableMap(expanded);
    }

    private String readFile(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length >= 2) {
                if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
                    return new String(bytes, 2, bytes.length - 2, java.nio.charset.StandardCharsets.UTF_16LE).stripTrailing();
                }
                if (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
                    return new String(bytes, 2, bytes.length - 2, java.nio.charset.StandardCharsets.UTF_16BE).stripTrailing();
                }
            }
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8).stripTrailing();
        } catch (IOException e) {
            log.debug("Cannot read secret file {}: {}", path, e.getMessage());
            return null;
        }
    }
}
