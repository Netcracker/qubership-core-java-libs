package com.netcracker.cloud.dbaas.client.service.mountedsecret;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Reads database connection properties from Secrets mounted at a fixed path instead of calling
 * dbaas over REST. This is the Java port of the Go {@code mountedSecretProvider}.
 * <p>
 * Each mounted Secret is a directory with two keys:
 * <ul>
 *     <li>{@code metadata.json} — the descriptor ({classifier, type, userRole, id, name, namespace, settings});</li>
 *     <li>{@code connectionProperties.json} — the raw connection-properties map.</li>
 * </ul>
 * A startup scan indexes every descriptor by its canonical {@code (classifier, type, role)} key.
 * {@link #resolve} re-reads {@code connectionProperties.json} fresh on every call (so a rotated
 * password is picked up at the next refetch), and returns empty on a miss so the caller falls
 * through to REST. The path set is fixed by the Deployment, so the index is only refreshed by a
 * throttled re-scan on a miss (a new Secret added without a redeploy) — there is no watcher.
 */
@Slf4j
public class MountedSecretSource {

    // Fixed mount path established by the dbaas-operator Deployment contract, not a configurable URI.
    @SuppressWarnings("java:S1075")
    static final String DEFAULT_PATH = "/etc/secrets/dbaas-secrets";
    static final String METADATA_FILE = "metadata.json";
    static final String CONNECTION_PROPERTIES_FILE = "connectionProperties.json";
    static final Duration RESCAN_THROTTLE = Duration.ofSeconds(60);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final Path basePath;
    private final Duration rescanThrottle;
    private final ReentrantLock rescanLock = new ReentrantLock();

    private final AtomicReference<Map<String, IndexEntry>> index = new AtomicReference<>(Collections.emptyMap());
    private volatile Instant lastRescan = Instant.EPOCH;

    public MountedSecretSource() {
        this(DEFAULT_PATH);
    }

    public MountedSecretSource(String basePath) {
        this(basePath, RESCAN_THROTTLE);
    }

    // Package-private: lets tests drive the throttled re-scan deterministically (e.g. Duration.ZERO).
    MountedSecretSource(String basePath, Duration rescanThrottle) {
        this.basePath = Path.of(basePath);
        this.rescanThrottle = rescanThrottle;
        buildIndex();
    }

    private record IndexEntry(Path dir, SecretMetadata meta) {
    }

    /** The connection properties read from a matched Secret plus its descriptor. */
    public record Resolved(Map<String, Object> connectionProperties, SecretMetadata metadata) {
    }

    /**
     * Looks up a mounted Secret for {@code (classifier, type, role)} and reads its connection
     * properties fresh. Returns empty on a miss — the caller must then fall back to REST.
     */
    public Optional<Resolved> resolve(Map<String, Object> classifier, String type, String role) {
        String key = ClassifierMatcher.matchingKey(classifier, type, role);

        IndexEntry entry = index.get().get(key);
        if (entry == null) {
            if (rescanDue()) {
                rescanThrottled();
                entry = index.get().get(key);
            }
            if (entry == null) {
                return Optional.empty();
            }
        }

        Path propsPath = entry.dir().resolve(CONNECTION_PROPERTIES_FILE);
        byte[] data;
        try {
            data = Files.readAllBytes(propsPath);
        } catch (NoSuchFileException e) {
            // Secret directory was removed; evict the stale entry so we don't keep hitting it.
            evict(key, entry);
            log.warn("mounted-secret: secret removed from disk, evicting index entry for {}", entry.dir());
            return Optional.empty();
        } catch (IOException e) {
            log.warn("mounted-secret: cannot read {} in {}: {}", CONNECTION_PROPERTIES_FILE, entry.dir(), e.toString());
            return Optional.empty();
        }

        Map<String, Object> props;
        try {
            props = MAPPER.readValue(data, MAP_TYPE);
        } catch (IOException e) {
            log.warn("mounted-secret: corrupt {} in {}: {}", CONNECTION_PROPERTIES_FILE, entry.dir(), e.toString());
            return Optional.empty();
        }

        log.debug("mounted-secret: hit for type={} classifier={} role={}", type, classifier, role);
        return Optional.of(new Resolved(props, entry.meta()));
    }

    private boolean rescanDue() {
        return Duration.between(lastRescan, Instant.now()).compareTo(rescanThrottle) >= 0;
    }

    private void rescanThrottled() {
        // tryLock collapses concurrent misses into a single re-scan (singleflight-style).
        if (!rescanLock.tryLock()) {
            return;
        }
        try {
            if (rescanDue()) {
                buildIndex();
            }
        } finally {
            rescanLock.unlock();
        }
    }

    private void evict(String key, IndexEntry expected) {
        rescanLock.lock();
        try {
            IndexEntry current = index.get().get(key);
            if (current == null || !current.equals(expected)) {
                return;
            }
            Map<String, IndexEntry> copy = new HashMap<>(index.get());
            copy.remove(key);
            index.set(Collections.unmodifiableMap(copy));
        } finally {
            rescanLock.unlock();
        }
    }

    private void buildIndex() {
        Map<String, IndexEntry> newIndex = new HashMap<>();
        try (DirectoryStream<Path> dirs = Files.newDirectoryStream(basePath)) {
            for (Path dir : dirs) {
                if (Files.isDirectory(dir)) {
                    indexSecretDir(dir, newIndex);
                }
            }
        } catch (NoSuchFileException e) {
            log.debug("mounted-secret: secret path {} not present, skipping (falling back to REST)", basePath);
        } catch (IOException e) {
            log.warn("mounted-secret: cannot read secret path {}: {}", basePath, e.toString());
        }
        this.index.set(Collections.unmodifiableMap(newIndex));
        this.lastRescan = Instant.now();
    }

    private void indexSecretDir(Path dir, Map<String, IndexEntry> newIndex) {
        byte[] data;
        try {
            data = Files.readAllBytes(dir.resolve(METADATA_FILE));
        } catch (IOException e) {
            // No metadata.json — not a dbaas secret, skip silently.
            return;
        }

        SecretMetadata meta;
        try {
            meta = MAPPER.readValue(data, SecretMetadata.class);
        } catch (IOException e) {
            log.warn("mounted-secret: corrupt {} in {}: {}", METADATA_FILE, dir, e.toString());
            return;
        }

        if (meta.getClassifier() == null || meta.getClassifier().isEmpty()
                || meta.getType() == null || meta.getType().isEmpty()) {
            log.warn("mounted-secret: incomplete metadata in {} (missing classifier or type), skipping", dir);
            return;
        }

        String key = ClassifierMatcher.matchingKey(meta.getClassifier(), meta.getType(), meta.getUserRole());
        IndexEntry existing = newIndex.put(key, new IndexEntry(dir, meta));
        if (existing != null) {
            log.warn("mounted-secret: duplicate key in {} and {} — second entry wins; check operator configuration",
                    existing.dir(), dir);
        }
    }
}
