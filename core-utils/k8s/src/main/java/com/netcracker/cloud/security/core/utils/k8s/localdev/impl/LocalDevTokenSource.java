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
import java.util.function.Supplier;

import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.MICROSERVICE_NAME_ENV;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.MICROSERVICE_NAME_PROPERTY;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.NAMESPACE_ENV;

@Slf4j
public final class LocalDevTokenSource implements TokenSource {

    private static final Duration EXPIRY_SKEW = Duration.ofMinutes(5);

    private final Supplier<TokenRequestClient> clientSupplier;
    private final Supplier<String> microserviceNameSupplier;
    private final Supplier<String> namespaceSupplier;
    private final ConcurrentMap<String, CachedToken> cache = new ConcurrentHashMap<>();

    private final AtomicReference<TokenRequestClient> client = new AtomicReference<>();

    public LocalDevTokenSource() {
        this(
                () -> new TokenRequestClient(new KubeConfigLoader().load()),
                defaultMicroserviceNameSupplier(),
                defaultNamespaceSupplier());
    }

    LocalDevTokenSource(
            Supplier<TokenRequestClient> clientSupplier,
            Supplier<String> microserviceNameSupplier,
            Supplier<String> namespaceSupplier) {
        this.clientSupplier = Objects.requireNonNull(clientSupplier, "clientSupplier");
        this.microserviceNameSupplier = Objects.requireNonNull(microserviceNameSupplier, "microserviceNameSupplier");
        this.namespaceSupplier = Objects.requireNonNull(namespaceSupplier, "namespaceSupplier");
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
            TokenRequestClient.TokenRequestResult result = request(audience);
            cache.put(audience, new CachedToken(result.token(), result.expiresAt().minus(EXPIRY_SKEW)));
            return result.token();
        }
    }

    private TokenRequestClient.TokenRequestResult request(String audience) {
        String namespace = requireNamespace();
        String serviceAccount = requireMicroserviceName();
        log.info("Local-dev TokenSource active: requesting token for audience={}, sa={}, namespace={}",
                audience, serviceAccount, namespace);
        return client().requestToken(namespace, serviceAccount, audience);
    }

    private static Supplier<String> defaultMicroserviceNameSupplier() {
        return () -> StringUtils.firstNonBlank(
                System.getProperty(MICROSERVICE_NAME_PROPERTY),
                System.getenv(MICROSERVICE_NAME_ENV));
    }

    private static Supplier<String> defaultNamespaceSupplier() {
        return () -> System.getenv(NAMESPACE_ENV);
    }

    private String requireMicroserviceName() {
        String name = microserviceNameSupplier.get();
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
        String namespace = namespaceSupplier.get();
        if (StringUtils.isBlank(namespace)) {
            throw new IllegalStateException(
                    "Local-dev M2M requires env '" + NAMESPACE_ENV
                            + "' with the Kubernetes namespace of the service account.");
        }
        return namespace.trim();
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

    @Override
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
