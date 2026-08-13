package com.netcracker.cloud.security.core.utils.k8s.localdev.impl;

import com.netcracker.cloud.security.core.utils.k8s.TokenSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.MICROSERVICE_NAME_ENV;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.MICROSERVICE_NAME_PROPERTY;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.NAMESPACE_ENV;

@Slf4j
public final class LocalDevTokenSource implements TokenSource {

    private static final Duration EXPIRY_SKEW = Duration.ofMinutes(5);

    private final String microserviceName;
    private final String namespace;
    private final ConcurrentMap<String, CachedToken> cache = new ConcurrentHashMap<>();
    private final AtomicReference<TokenRequestClient> client = new AtomicReference<>();

    public LocalDevTokenSource() {
        this.microserviceName = null;
        this.namespace = null;
    }

    LocalDevTokenSource(TokenRequestClient client, String microserviceName, String namespace) {
        this.client.set(Objects.requireNonNull(client, "client"));
        this.microserviceName = microserviceName;
        this.namespace = namespace;
    }

    @Override
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
            String serviceAccount = requireMicroserviceName();
            String resolvedNamespace = requireNamespace();
            log.info("Local-dev TokenSource active: requesting token for audience={}, sa={}, namespace={}",
                    audience, serviceAccount, resolvedNamespace);
            TokenRequestClient.TokenRequestResult result =
                    client().requestToken(resolvedNamespace, serviceAccount, audience);
            cache.put(audience, new CachedToken(result.token(), result.expiresAt().minus(EXPIRY_SKEW)));
            return result.token();
        }
    }

    private String requireMicroserviceName() {
        String name = StringUtils.firstNonBlank(
                microserviceName,
                System.getProperty(MICROSERVICE_NAME_PROPERTY),
                System.getenv(MICROSERVICE_NAME_ENV));
        if (StringUtils.isBlank(name)) {
            throw new IllegalStateException(
                    "Local-dev M2M requires '" + MICROSERVICE_NAME_PROPERTY
                            + "' (system property or " + MICROSERVICE_NAME_ENV
                            + " env). Set it in application config and ensure framework bootstrap runs, "
                            + "or pass -D" + MICROSERVICE_NAME_PROPERTY + "=<service-account-name>.");
        }
        return name.trim();
    }

    private String requireNamespace() {
        String resolved = StringUtils.firstNonBlank(namespace, System.getenv(NAMESPACE_ENV));
        if (StringUtils.isBlank(resolved)) {
            throw new IllegalStateException(
                    "Local-dev M2M requires env '" + NAMESPACE_ENV
                            + "' with the Kubernetes namespace of the service account.");
        }
        return resolved.trim();
    }

    private TokenRequestClient client() {
        TokenRequestClient existing = client.get();
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            existing = client.get();
            if (existing == null) {
                existing = new TokenRequestClient(new KubeConfigLoader().load());
                client.set(existing);
            }
            return existing;
        }
    }

    @Override
    public void close() {
        cache.clear();
    }

    private record CachedToken(String token, Instant refreshAfter) {
        boolean isValid() {
            return Instant.now().isBefore(refreshAfter);
        }
    }
}
