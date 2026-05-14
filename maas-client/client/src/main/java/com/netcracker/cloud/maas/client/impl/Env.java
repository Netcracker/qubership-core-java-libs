package com.netcracker.cloud.maas.client.impl;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Env {
    private static final Logger log = LoggerFactory.getLogger(Env.class);

    static final String ENV_NAMESPACE = "NAMESPACE";
    static final String ENV_CLOUD_NAMESPACE = "CLOUD_NAMESPACE";
    static final String ENV_ORIGIN_NAMESPACE = "ORIGIN_NAMESPACE";
    static final String ENV_MICROSERVICE_NAME = "MICROSERVICE_NAME";
    static final String ENV_K8S_ENABLED = "SECURITY_M2M_KUBERNETES_ENABLED";

    public static final String PROP_CLOUD_NAMESPACE = "cloud.microservice.namespace";
    public static final String PROP_NAMESPACE = "maas.client.classifier.namespace"; //todo deprecated - delete in the next major release
    public static final String PROP_ORIGIN_NAMESPACE = "origin_namespace"; //todo change to 'origin.namespace'
    public static final String PROP_MAAS_AGENT_URL = "maas.client.api.url";
    public static final String PROP_MAAS_URL = "maas.internal.address";
    public static final String PROP_API_AUTH = "maas.client.api.auth";
    public static final String PROP_TENANT_MANAGER_URL = "maas.client.tenant-manager.url";
    public static final String PROP_TENANT_MANAGER_RECONNECT_TIMEOUT = "maas.client.tenant-manager.reconnect-timeout";
    public static final String PROP_HTTP_TIMEOUT = "maas.http.timeout";

    public static String apiUrl() {
        boolean k8sEnabled = Boolean.parseBoolean(System.getenv().get(ENV_K8S_ENABLED));
        String maasAgentUrl = stringProperty(PROP_MAAS_AGENT_URL)
                .map(Env::normalizeUrl)
                .orElse(addr2http("maas-agent"));
        if(!k8sEnabled) {
            return maasAgentUrl;
        }
        return stringProperty(PROP_MAAS_URL)
                .map(Env::normalizeUrl)
                .orElseGet(() -> {
                    log.warn("MaaS address is not available, falling back to maas-agent. Specify '{}'property to MaaS url", PROP_MAAS_URL);
                    return maasAgentUrl;
                });
    }

    public static String apiAuth() {
        return stringProperty(PROP_API_AUTH).orElse("Bearer");
    }

    static Args namespaceProps = new Args(args(PROP_CLOUD_NAMESPACE, PROP_NAMESPACE), args(ENV_CLOUD_NAMESPACE, ENV_NAMESPACE),
            args(new Deprecated(PROP_NAMESPACE, PROP_CLOUD_NAMESPACE)));

    static Args originNamespaceProps = new Args(args(PROP_ORIGIN_NAMESPACE, "origin.namespace"), args(ENV_ORIGIN_NAMESPACE),
            args(new Deprecated(PROP_ORIGIN_NAMESPACE, "origin.namespace")));

    public static String namespace() {
        return getProps(Env::getPropsOrEnvsMust, namespaceProps);
    }

    public static Optional<String> namespaceOpt() {
        return getProps(Env::getPropsOrEnvs, namespaceProps);
    }

    public static String originNamespace() {
        return getProps(Env::getPropsOrEnvsMust, originNamespaceProps);
    }

    public static Optional<String> originNamespaceOpt() {
        return getProps(Env::getPropsOrEnvs, originNamespaceProps);
    }

    public static String microserviceName() {
        return getPropsOrEnvsMust(args(), args(ENV_MICROSERVICE_NAME));
    }

    public static String tenantManagerUrl() {
        return stringProperty(PROP_TENANT_MANAGER_URL)
                .map(Env::normalizeUrl)
                .orElse(addr2http("tenant-manager"));
    }

    public static long tenantManagerReconnectTimeout() {
        return Duration.parse(
                stringProperty(PROP_TENANT_MANAGER_RECONNECT_TIMEOUT).orElse("PT15S")
        ).toMillis();
    }

    public static Duration httpTimeout() {
        return Duration.ofSeconds(
                stringProperty(PROP_HTTP_TIMEOUT)
                        .map(Integer::parseInt)
                        .orElse(30)
        );
    }

    public static String url2ws(String url) {
        return url.replaceAll("^http(s?):", "ws$1:");
    }

    @SneakyThrows
    public static String tenantManagerHost() {
        var url = new URL(tenantManagerUrl());
        return url.getHost() + (url.getPort() != -1 ? ":" + url.getPort() : "");
    }

    private static String addr2http(String addr) {
        return String.format("http://%s:8080", addr);
    }

    private static String normalizeUrl(String value) {
        try {
            var url = new URL(value);
            return url.getProtocol() + "://" + url.getHost()
                    + (url.getPort() != -1 ? ":" + url.getPort() : "");
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static <T> T getProps(PropsOrEnvsSupplier<T> supplier, Args args) {
        return supplier.getPropsOrEnvs(args.props(), args.envs(), args.deprecated());
    }

    private record Args(String[] props, String[] envs, Deprecated[] deprecated) {
    }

    private record Deprecated(String deprecated, String valid) {
    }

    private interface PropsOrEnvsSupplier<T> {
        T getPropsOrEnvs(String[] props, String[] envs, Deprecated[] deprecated);
    }

    @SafeVarargs
    private static <T> T[] args(T... args) {
        return args;
    }

    private static String getPropsOrEnvsMust(String[] props, String[] envs, Deprecated... deprecated) {
        BiFunction<String, String[], String> argsFunc = (name, args) -> args.length > 0 ? String.format("%s(s): %s", name, String.join(",", args)) : "";
        return getPropsOrEnvs(props, envs, deprecated).orElseThrow(() -> {
            String propsMsg = argsFunc.apply("prop", props);
            String envsMsg = argsFunc.apply("env", envs);
            String msg = String.format("Missing required %s%s%s",
                    propsMsg, !propsMsg.isEmpty() && !envsMsg.isEmpty() ? " or " : "", envsMsg);
            return new IllegalStateException(msg);
        });
    }

    private static Optional<String> getPropsOrEnvs(String[] props, String[] envs, Deprecated... deprecated) {
        Map<String, String> deprecatedMap = Arrays.stream(deprecated).collect(Collectors.toMap(Deprecated::deprecated, Deprecated::valid));
        BiFunction<String[], Function<String, Optional<String>>, Optional<String>> func = (names, f) -> Arrays.stream(names)
                .map(arg -> f.apply(arg).map(value -> {
                    log.debug("Resolved '{}' to '{}'", arg, value);
                    Optional.ofNullable(deprecatedMap.get(arg))
                            .ifPresent(alternative -> log.warn("Using '{}' is deprecated. Migrate to '{}'", arg, alternative));
                    return value;
                }).orElse(null))
                .filter(Objects::nonNull)
                .findFirst();
        return func.apply(props, Env::propertyWithConfigFallback).or(() -> func.apply(envs, Env::envWithConfigFallback));
    }

    /**
     * Quarkus/SmallRye load {@code .env} into MicroProfile Config, not into {@link System#getenv()} or {@link System#getProperty(String)}.
     * Optional MP Config lookup keeps this library working in Quarkus without a compile dependency on the MP Config API.
     */
    private static Optional<String> propertyWithConfigFallback(String key) {
        return Optional.ofNullable(System.getProperty(key)).or(() -> microProfileConfigOptional(key));
    }

    private static Optional<String> envWithConfigFallback(String key) {
        return Optional.ofNullable(System.getenv(key)).or(() -> microProfileConfigOptional(key));
    }

    private static Optional<String> stringProperty(String key) {
        return propertyWithConfigFallback(key);
    }

    @SuppressWarnings("unchecked")
    private static Optional<String> microProfileConfigOptional(String key) {
        try {
            Class<?> providerClass = Class.forName("org.eclipse.microprofile.config.ConfigProvider");
            Method getConfig = providerClass.getMethod("getConfig");
            Object config = getConfig.invoke(null);
            Method getOptionalValue = config.getClass().getMethod("getOptionalValue", String.class, Class.class);
            return (Optional<String>) getOptionalValue.invoke(config, key, String.class);
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        } catch (Throwable e) {
            log.trace("MicroProfile Config not available or lookup failed for '{}'", key, e);
            return Optional.empty();
        }
    }
}
