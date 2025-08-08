package org.qubership.cloud.maas.client.impl;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static final String PROP_CLOUD_NAMESPACE = "cloud.microservice.namespace";
    public static final String PROP_NAMESPACE = "maas.client.classifier.namespace"; //todo deprecated - delete in the next major release
    public static final String PROP_ORIGIN_NAMESPACE = "origin_namespace"; //todo change to 'origin.namespace'
    public static final String PROP_API_URL = "maas.client.api.url";
    public static final String PROP_API_AUTH = "maas.client.api.auth";
    public static final String PROP_TENANT_MANAGER_URL = "maas.client.tenant-manager.url";
    public static final String PROP_TENANT_MANAGER_RECONNECT_TIMEOUT = "maas.client.tenant-manager.reconnect-timeout";
    public static final String PROP_HTTP_TIMEOUT = "maas.http.timeout";

    public static String apiUrl() {
        return Optional.ofNullable(System.getProperty(PROP_API_URL))
                .map(Env::normalizeUrl)
                .orElse(addr2http("maas-agent"));
    }

    public static String apiAuth() {
        return Optional.ofNullable(System.getProperty(PROP_API_AUTH)).orElse("Bearer");
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
        return Optional.ofNullable(System.getProperty(PROP_TENANT_MANAGER_URL))
                .map(Env::normalizeUrl)
                .orElse(addr2http("tenant-manager"));
    }

    public static long tenantManagerReconnectTimeout() {
        return Duration.parse(
                Optional.ofNullable(System.getProperty(PROP_TENANT_MANAGER_RECONNECT_TIMEOUT))
                        .orElse("PT15S")
        ).toMillis();
    }

    public static Duration httpTimeout() {
        return Duration.ofSeconds(
                Optional.ofNullable(System.getProperty(PROP_HTTP_TIMEOUT))
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
        BiFunction<String[], Function<String, String>, Optional<String>> func = (names, f) -> Arrays.stream(names)
                .map(arg -> {
                    String value = f.apply(arg);
                    if (value != null) {
                        log.debug("Resolved '{}' to '{}'", arg, value);
                        Optional.ofNullable(deprecatedMap.get(arg))
                                .ifPresent(alternative -> log.warn("Using '{}' is deprecated. Migrate to '{}'", arg, alternative));
                    }
                    return value;
                })
                .filter(Objects::nonNull)
                .findFirst();
        return func.apply(props, System::getProperty).or(() -> func.apply(envs, System::getenv));
    }
}
