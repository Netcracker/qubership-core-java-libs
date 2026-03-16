package com.netcracker.cloud.consul.provider.spring.common.config;

import org.apache.commons.logging.Log;
import com.netcracker.cloud.consul.provider.common.TokenProvider;
import com.netcracker.cloud.consul.provider.common.client.ConsulRestClient;
import com.netcracker.cloud.consul.provider.spring.common.Utils;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.security.core.auth.M2MManager;
import org.springframework.boot.bootstrap.BootstrapRegistry;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.cloud.consul.ConsulProperties;
import org.springframework.cloud.consul.config.ConsulConfigDataLocationResolver;
import org.springframework.cloud.consul.config.ConsulConfigProperties;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;

import java.io.IOException;
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

    protected ConsulM2MConfigDataLocationResolver(DeferredLogFactory log) {
        super(log);
        this.log = log.getLog(ConsulM2MConfigDataLocationResolver.class);
    }

    @Override
    protected ConsulConfigProperties loadConfigProperties(ConfigDataLocationResolverContext resolverContext) {
        ConsulConfigProperties consulConfigProperties = super.loadConfigProperties(resolverContext);
        boolean isConsulM2MEnabled = resolverContext.getBinder().bind(PROP_CONSUL_M2M_ENABLED, Boolean.class).orElse(true);
        if (!isConsulM2MEnabled) {
            return consulConfigProperties;
        }
        ConsulProperties properties = resolverContext.getBootstrapContext().get(ConsulProperties.class);
        try {
            M2MManager m2MManager = resolverContext.getBootstrapContext().get(M2MManager.class);
            ConsulRestClient client = createConsulRestClient(Utils.formatConsulAddress(properties), () -> m2MManager.getToken().getTokenValue());
            TokenProvider tokenProvider = new TokenProvider(client, getPropsOrEnvsMust(args(PROP_CLOUD_NAMESPACE), args(ENV_NAMESPACE, ENV_CLOUD_NAMESPACE)));
            consulConfigProperties.setAclToken(tokenProvider.getNewConsulToken().getSecretId());
        } catch (IOException e) {
            log.error("can not get consul token by m2m: ", e);
        }
        registerAndPromoteBean(resolverContext, ConsulProperties.class, BootstrapRegistry.InstanceSupplier.of(properties));
        return consulConfigProperties;
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


