package com.netcracker.cloud.consul.provider.spring.common.config;

import org.apache.commons.logging.Log;
import com.netcracker.cloud.consul.provider.common.ConsulLoginMode;
import com.netcracker.cloud.consul.provider.common.ConsulTokenProvider;
import com.netcracker.cloud.consul.provider.common.TokenStorageFactory;
import com.netcracker.cloud.consul.provider.common.client.ConsulRestClient;
import com.netcracker.cloud.consul.provider.spring.common.TokenRefusals;
import com.netcracker.cloud.consul.provider.spring.common.Utils;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.security.core.auth.M2MManager;
import org.springframework.boot.bootstrap.BootstrapContext;
import org.springframework.boot.bootstrap.BootstrapRegistry;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.cloud.consul.ConsulAutoConfiguration;
import org.springframework.cloud.consul.ConsulClient;
import org.springframework.cloud.consul.ConsulProperties;
import org.springframework.cloud.consul.config.ConsulConfigDataLocationResolver;
import org.springframework.cloud.consul.config.ConsulConfigProperties;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.util.UriComponents;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class ConsulM2MConfigDataLocationResolver extends ConsulConfigDataLocationResolver {

    public static final String PROP_CLOUD_NAMESPACE = "cloud.microservice.namespace";
    public static final String PROP_CONSUL_M2M_ENABLED = "spring.cloud.consul.config.m2m.enabled";
    static final String ENV_NAMESPACE = "NAMESPACE";
    static final String ENV_CLOUD_NAMESPACE = "CLOUD_NAMESPACE";

    private final Log log;
    private final TokenRefusals refusals = new TokenRefusals();

    protected ConsulM2MConfigDataLocationResolver(DeferredLogFactory log) {
        super(log);
        this.log = log.getLog(ConsulM2MConfigDataLocationResolver.class);
    }

    /**
     * Logs in once and writes the {@code SecretID} into {@link ConsulConfigProperties}, so that Consul is readable
     * before the application context exists. The phase runs without a context, so the mode is bound through {@link
     * Binder} rather than injected.
     *
     * <p>Only the login is guarded: any failure of it is logged rather than thrown, and the application starts without
     * an ACL token for the {@code TokenStorage} bean to obtain. An unusable configuration ends the phase — no attempt
     * fixes it.
     */
    @Override
    protected ConsulConfigProperties loadConfigProperties(ConfigDataLocationResolverContext resolverContext) {
        ConsulConfigProperties consulConfigProperties = super.loadConfigProperties(resolverContext);
        Binder binder = resolverContext.getBinder();
        boolean isConsulM2MEnabled = binder.bind(PROP_CONSUL_M2M_ENABLED, Boolean.class).orElse(true);
        if (!isConsulM2MEnabled) {
            return consulConfigProperties;
        }
        ConsulLoginProperties login = binder.bind(ConsulLoginProperties.PREFIX, ConsulLoginProperties.class)
                .orElseGet(ConsulLoginProperties::new);
        ConsulProperties properties = resolverContext.getBootstrapContext().get(ConsulProperties.class);
        Supplier<String> m2mTokenSupplier = () ->
                resolverContext.getBootstrapContext().get(M2MManager.class).getToken().getTokenValue();
        String consulAddress = Utils.formatConsulAddress(properties);
        ConsulRestClient client = createConsulRestClient(consulAddress, m2mTokenSupplier);

        TokenStorageFactory.CreateOptions.Builder options = login.toOptionsBuilder().consulUrl(consulAddress);
        if (login.getMode() != ConsulLoginMode.KUBERNETES) {
            options.namespace(getPropsOrEnvsMust(args(PROP_CLOUD_NAMESPACE), args(ENV_NAMESPACE, ENV_CLOUD_NAMESPACE)))
                    .m2mSupplier(m2mTokenSupplier);
        }
        ConsulTokenProvider tokenProvider = TokenStorageFactory.from(client, options.build());

        try {
            consulConfigProperties.setAclToken(tokenProvider.getToken().getSecretId());
        } catch (Exception e) {
            log.error("can not get consul token: ", e);
        }
        registerAndPromoteBean(resolverContext, ConsulProperties.class, BootstrapRegistry.InstanceSupplier.of(properties));
        registerAndPromoteBean(resolverContext, TokenRefusals.class, BootstrapRegistry.InstanceSupplier.of(refusals));
        return consulConfigProperties;
    }

    /**
     * Builds the client of the ConfigData phase over an exchange adapter that reports a refused ACL token, so that the
     * pod stops sending one Consul no longer resolves.
     *
     * <p>The seam is here rather than in a bean of the application context: the client this method returns is promoted
     * into the context as a manual singleton, which no {@code BeanPostProcessor} reaches and no second bean may shadow
     * without making the injection into the config watch ambiguous. What the superclass builds is assembled here from
     * the same two factory methods it calls, with the adapter wrapped in between.
     */
    @Override
    protected ConsulClient createConsulClient(BootstrapContext context) {
        ConsulProperties properties = context.get(ConsulProperties.class);
        try {
            HttpExchangeAdapter adapter = ConsulAutoConfiguration.createConsulClientSettings(
                    ConsulAutoConfiguration.createConsulClientBaseUrl(properties), properties.getTls()).adapter();
            return ConsulAutoConfiguration.createNewConsulClient(
                    new RefusedTokenReportingAdapter(adapter, refusals));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    @Override
    protected UriComponents parseLocation(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
        String originalLocation = location.getNonPrefixedValue(PREFIX);
        if (!StringUtils.hasText(originalLocation)) {
            return null;
        }

        return super.parseLocation(context, ConfigDataLocation.of(originalLocation.replaceAll("^.+//", "")));
    }

    protected ConsulRestClient createConsulRestClient(String consulAddr, Supplier<String> m2mTokenSupplier) {
        return new ConsulRestClient(createMicroserviceRestClient(), consulAddr, m2mTokenSupplier);
    }

    abstract protected MicroserviceRestClient createMicroserviceRestClient();

    @SafeVarargs
    protected static <T> T[] args(T... args) {
        return args;
    }

    protected static String getPropsOrEnvsMust(String[] props, String[] envs) {
        BiFunction<String, String[], String> argsFunc = (name, args) -> args.length > 0 ? String.format("%s(s): [%s]", name, String.join(", ", args)) : "";

        BiFunction<String[], Function<String, String>, Optional<String>> func = (names, f) ->
                Arrays.stream(names).map(f).filter(Objects::nonNull).findFirst();

        return func.apply(props, System::getProperty).or(() -> func.apply(envs, System::getenv)).orElseThrow(() -> {
            String propsMsg = argsFunc.apply("prop", props);
            String envsMsg = argsFunc.apply("env", envs);
            String msg = String.format("Missing required %s%s%s",
                    propsMsg, !propsMsg.isEmpty() && !envsMsg.isEmpty() ? " or " : "", envsMsg);
            return new IllegalArgumentException(msg);
        });
    }
}


