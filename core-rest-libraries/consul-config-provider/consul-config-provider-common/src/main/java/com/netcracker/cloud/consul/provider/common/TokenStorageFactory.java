package com.netcracker.cloud.consul.provider.common;

import com.netcracker.cloud.consul.provider.common.client.ConsulClient;
import com.netcracker.cloud.security.core.utils.k8s.AudienceName;

import java.util.Locale;
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
        TokenUpdater tokenUpdater = new TokenUpdater(from(consulClient, config), new SelfTokenReader(consulClient));
        TokenStorage tokenStorage = createTokenStorage(config);
        tokenUpdater.watch(tokenStorage::update, tokenStorage.get());
        return tokenStorage;
    }

    public static ConsulLogin from(ConsulClient client, CreateOptions options) {
        switch (options.mode) {
            case M2M:
                return m2mLogin(client, options);
            case KUBERNETES:
                return kubernetesLogin(client, options);
            default:
                return new ProbingConsulLogin(kubernetesLogin(client, options), m2mLogin(client, options));
        }
    }

    private static ConsulLogin m2mLogin(ConsulClient client, CreateOptions options) {
        return new TokenProvider(client, new M2MLoginCredentials(options.namespace, options.m2mSupplier));
    }

    private static ConsulLogin kubernetesLogin(ConsulClient client, CreateOptions options) {
        return new TokenProvider(client, new KubernetesLoginCredentials(options.authMethod, options.audience));
    }

    abstract protected TokenStorage createTokenStorage(CreateOptions config);

    abstract protected ConsulClient createTokenExchanger(CreateOptions config);

    public static class CreateOptions {

        public static final String DEFAULT_AUTH_METHOD = "kubernetes-auth-method-placeholder";

        String consulUrl;
        String namespace;
        Supplier<String> m2mSupplier;
        ConsulLoginMode mode;
        String authMethod;
        String audience;

        public ConsulLoginMode getMode() {
            return mode;
        }

        public String getAuthMethod() {
            return authMethod;
        }

        public String getAudience() {
            return audience;
        }

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

            public Builder mode(ConsulLoginMode mode) {
                options.mode = mode;
                return this;
            }

            public Builder authMethod(String authMethod) {
                options.authMethod = authMethod;
                return this;
            }

            public Builder audience(String audience) {
                options.audience = audience;
                return this;
            }

            public CreateOptions build() {
                if (options.mode == null) {
                    options.mode = ConsulLoginMode.KUBERNETES_WITH_M2M_FALLBACK;
                }
                if (options.authMethod == null) {
                    options.authMethod = DEFAULT_AUTH_METHOD;
                }
                if (options.audience == null) {
                    options.audience = AudienceName.NETCRACKER;
                }
                require(options.consulUrl != null, "consulUrl", options.mode);
                if (options.mode != ConsulLoginMode.KUBERNETES) {
                    require(options.namespace != null, "namespace", options.mode);
                    require(options.m2mSupplier != null, "m2mSupplier", options.mode);
                }
                CreateOptions result = options;
                options = new CreateOptions();
                return result;
            }

            private static void require(boolean given, String input, ConsulLoginMode mode) {
                if (!given) {
                    throw new IllegalArgumentException(String.format("%s is required in the %s consul login mode",
                            input, mode.name().toLowerCase(Locale.ROOT).replace('_', '-')));
                }
            }
        }
    }
}
