package com.netcracker.cloud.security.core.utils.k8s.localdev;

import com.netcracker.cloud.security.core.utils.k8s.Priority;
import com.netcracker.cloud.security.core.utils.k8s.TokenSource;
import com.netcracker.cloud.security.core.utils.k8s.impl.CachingTokenSource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

@Slf4j
@Priority(100)
public class LocalDevTokenSource implements TokenSource {

    private static final Duration EXPIRY_SKEW = Duration.ofMinutes(5);

    private final TokenSource fallback;
    private final Supplier<TokenRequestClient> clientSupplier;
    private final ConcurrentMap<String, CachedToken> cache = new ConcurrentHashMap<>();

    private volatile TokenRequestClient client;

    public LocalDevTokenSource() {
        this(new CachingTokenSource(), () -> new TokenRequestClient(KubeConfigLoader.load()));
    }

    @VisibleForTesting
    LocalDevTokenSource(TokenSource fallback, Supplier<TokenRequestClient> clientSupplier) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.clientSupplier = Objects.requireNonNull(clientSupplier, "clientSupplier");
    }

    @Override
    public String getToken(String audience) {
        if (!LocalDevMode.isEnabled()) {
            return fallback.getToken(audience);
        }
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
        TokenRequestClient existing = client;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (client == null) {
                client = clientSupplier.get();
            }
            return client;
        }
    }

    @Override
    public void close() throws Exception {
        cache.clear();
        fallback.close();
    }

    private record CachedToken(String token, Instant refreshAfter) {
        boolean isValid() {
            return Instant.now().isBefore(refreshAfter);
        }
    }
}
