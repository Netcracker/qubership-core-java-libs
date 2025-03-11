package org.qubership.cloud.quarkus.consul.client;

import org.qubership.cloud.consul.provider.common.OkHttpTokenStorageFactory;
import org.qubership.cloud.consul.provider.common.TokenStorage;
import org.qubership.cloud.consul.provider.common.TokenStorageFactory;
import org.qubership.cloud.quarkus.security.auth.M2MManager;
import org.qubership.cloud.security.core.utils.tls.TlsUtils;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.UnlessBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import okhttp3.OkHttpClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

@Singleton
public class ConsulClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ConsulClientConfiguration.class);

    @Produces
    @DefaultBean
    @Singleton
    @Deprecated(since = "6.3.6", forRemoval = true)
    public com.ecwid.consul.v1.ConsulClient consulClient(
            @ConfigProperty(name = "quarkus.consul-source-config.agent.url") Optional<String> agentUrl) {
        log.warn("com.ecwid.consul.v1.ConsulClient is deprecated and will be removed. Use org.qubership.cloud.quarkus.consul.client instead.");
        Optional<URL> consulUrl = getURL(agentUrl);
        if (consulUrl.isEmpty()) {
            log.error("Cannot find consul agent url");
            return null;
        }
        return new com.ecwid.consul.v1.ConsulClient(consulUrl.get().getHost(), consulUrl.get().getPort());
    }

    @Produces
    @DefaultBean
    @Singleton
    public ConsulClient innerConsulClient(
            @ConfigProperty(name = "quarkus.consul-source-config.agent.url") Optional<String> agentUrl) {
        Optional<URL> consulUrl = getURL(agentUrl);
        if (consulUrl.isEmpty()) {
            log.error("Cannot find consul agent url");
            return null;
        }
        return new ConsulClient(String.valueOf(consulUrl.get()));
    }


    @Produces
    @ApplicationScoped
    @UnlessBuildProperty(name = "quarkus.consul-source-config.m2m.enabled", stringValue = "false", enableIfMissing = true)
    public TokenStorageFactory tokenStorageFactory() {
        return new OkHttpTokenStorageFactory(
                new OkHttpClient.Builder()
                        .sslSocketFactory(TlsUtils.getSslContext().getSocketFactory(), TlsUtils.getTrustManager())
                        .build());
    }

    @Produces
    @ApplicationScoped
    @UnlessBuildProperty(name = "quarkus.consul-source-config.m2m.enabled", stringValue = "false", enableIfMissing = true)
    public TokenStorage tokenStorage(TokenStorageFactory tokenStorageFactory,
                                     @ConfigProperty(name = "cloud.microservice.namespace") String namespace,
                                     @ConfigProperty(name = "quarkus.consul-source-config.agent.url") String agentUrl) {
        return tokenStorageFactory.create(new TokenStorageFactory.CreateOptions.Builder()
                .consulUrl(agentUrl)
                .namespace(namespace)
                .m2mSupplier(() -> M2MManager.getInstance().getToken().getTokenValue())
                .build());
    }

    @Produces
    @DefaultBean
    public TokenStorage noopTokenStorage() {
        return new TokenStorage() {
            @Override
            public String get() {
                return "";
            }

            @Override
            public void update(String s) {
                // nothing
            }
        };
    }

    private Optional<URL> getURL(Optional<String> urlOptional) {
        URL agentUrl;
        try {
            if (urlOptional.isEmpty()) {
                urlOptional = Optional.ofNullable(System.getenv("CONSUL_URL"));
            }
            String url = "";
            if (urlOptional.isPresent()) {
                url = urlOptional.get();
            }
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            agentUrl = new URL(url);
        } catch (MalformedURLException e) {
            log.error("Can not parse config.agent.url: ", e);
            return Optional.empty();
        }
        return Optional.of(agentUrl);
    }
}
