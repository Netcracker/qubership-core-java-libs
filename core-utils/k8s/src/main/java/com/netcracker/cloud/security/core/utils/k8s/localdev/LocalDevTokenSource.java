package com.netcracker.cloud.security.core.utils.k8s.localdev;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Slf4j
public class LocalDevTokenSource {

    private static final Duration EXPIRY_SKEW = Duration.ofMinutes(5);

    private final Supplier<TokenRequestClient> clientSupplier;
    private final ConcurrentMap<String, CachedToken> cache = new ConcurrentHashMap<>();

    private final AtomicReference<TokenRequestClient> client = new AtomicReference<>();

    public LocalDevTokenSource() {
        this(() -> new TokenRequestClient(KubeConfigLoader.load()));
    }

    @VisibleForTesting
    LocalDevTokenSource(Supplier<TokenRequestClient> clientSupplier) {
        this.clientSupplier = Objects.requireNonNull(clientSupplier, "clientSupplier");
    }

    public String getToken(String audience) {
        Objects.requireNonNull(audience, "audience");
        CachedToken cached = cache.get(audience);
        if (cached != null && cached.isValid()) {
            return cached.token();
        }
        synchronized (this) {
            cached = cache.get(audience);
            if (cached != null && cached.isValid()) {
                return cached.token();
            }
            TokenRequestClient.TokenRequestResult result = request(audience);
            cache.put(audience, new CachedToken(result.token(), result.expiresAt().minus(EXPIRY_SKEW)));
            return result.token();
        }
    }

    private TokenRequestClient.TokenRequestResult request(String audience) {
        String namespace = LocalDevMode.requireNamespace();
        String serviceAccount = LocalDevMode.requireMicroserviceName();
        log.info("Local-dev TokenSource active: requesting token for audience={}, sa={}, namespace={}",
                audience, serviceAccount, namespace);
        return client().requestToken(namespace, serviceAccount, audience);
    }

    private TokenRequestClient client() {
        TokenRequestClient existing = client.get();
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            TokenRequestClient resolved = client.get();
            if (resolved == null) {
                resolved = clientSupplier.get();
                client.set(resolved);
            }
            return resolved;
        }
    }

    public void close() {
        cache.clear();
        client.set(null);
    }

    private record CachedToken(String token, Instant refreshAfter) {
        boolean isValid() {
            return Instant.now().isBefore(refreshAfter);
        }
    }
}
