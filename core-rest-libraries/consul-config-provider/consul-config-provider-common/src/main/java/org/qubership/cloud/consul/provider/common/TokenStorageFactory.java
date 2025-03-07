package org.qubership.cloud.consul.provider.common;

import org.qubership.cloud.consul.provider.common.client.ConsulClient;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * {@link TokenStorageFactory} first gets Consul token then starts update token task. Implementations must
 * choose desired {@link TokenStorage} implementation to be created.
 */
public abstract class TokenStorageFactory {


    protected TokenStorageFactory() {
    }

    public TokenStorage create(CreateOptions config) {
        ConsulClient consulClient = createTokenExchanger(config);
        TokenUpdater tokenUpdater = new TokenUpdater(consulClient, config.namespace);
        TokenStorage tokenStorage = createTokenStorage(config);
        tokenUpdater.watch(tokenStorage::update, tokenStorage.get());
        return tokenStorage;
    }

    abstract protected TokenStorage createTokenStorage(CreateOptions config);

    abstract protected ConsulClient createTokenExchanger(CreateOptions config);

    public static class CreateOptions {
        String consulUrl;
        String namespace;
        Supplier<String> m2mSupplier;

        public static class Builder {
            CreateOptions options = new CreateOptions();

            public Builder consulUrl(String url) {
                options.consulUrl = url;
                if (options.consulUrl.endsWith("/")) {
                    options.consulUrl = options.consulUrl.substring(0, options.consulUrl.length() - 1);
                }
                return this;
            }

            public Builder namespace(String namespace) {
                options.namespace = namespace;
                return this;
            }

            public Builder m2mSupplier(Supplier<String> m2mTokenSupplier) {
                options.m2mSupplier = m2mTokenSupplier;
                return this;
            }

            public CreateOptions build() {
                Objects.requireNonNull(options.consulUrl);
                Objects.requireNonNull(options.namespace);
                Objects.requireNonNull(options.m2mSupplier);
                CreateOptions result = options;
                options = new CreateOptions();
                return result;
            }
        }
    }
}
