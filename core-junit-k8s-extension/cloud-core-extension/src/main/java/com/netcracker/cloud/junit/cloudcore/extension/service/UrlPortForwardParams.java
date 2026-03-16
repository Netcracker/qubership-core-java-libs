package com.netcracker.cloud.junit.cloudcore.extension.service;

import lombok.Builder;
import lombok.Getter;

import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class UrlPortForwardParams<T> implements BasePortForwardParams<T> {
    private final String serviceName;
    private final int port;
    private final String namespace;
    private final String host;
    private final Class<T> resultType;
    private final String scheme;

    public static final Pattern urlPattern = Pattern.compile("^((?<scheme>\\w+)://)?(?<host>[^:/]+)(:(?<port>\\d+))?$");
    public static final Pattern hostPattern = Pattern.compile("^(?<service>[^.\\s]+)(\\.(?<namespace>[^.\\s]+)(\\..+)*)?$");

    @Builder
    private UrlPortForwardParams(String host, String namespace, String serviceName, int port, Class<T> resultType, String scheme) {
        this.host = host;
        this.namespace = namespace;
        this.serviceName = serviceName;
        this.port = port;
        this.resultType = resultType;
        this.scheme = scheme;
    }

    public static UrlPortForwardParamsBuilder<NetSocketAddress> builder(String uri) {
        return builder(uri, NetSocketAddress.class);
    }

    public static UrlPortForwardParamsBuilder<URL> builderAsUrl(String uri) {
        return builder(uri, URL.class);
    }

    private static <T> UrlPortForwardParamsBuilder<T> builder(String uri, Class<T> resultType) {
        Matcher matcher = urlPattern.matcher(uri);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid uri for port-forward: " + uri + ". Must match pattern: " + urlPattern.pattern());
        }
        String host = matcher.group("host");
        String scheme = matcher.group("scheme");
        int port = Optional.ofNullable(matcher.group("port")).map(Integer::parseInt).orElseGet(() -> {
            if ("https".equals(scheme)) return 443;
            else return 80;
        });

        matcher = hostPattern.matcher(host);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid host for port-forward: " + host + ". Must match pattern: " + hostPattern.pattern());
        }
        String namespace = matcher.group("namespace");
        String serviceName = matcher.group("service");
        return new UrlPortForwardParamsBuilder<T>()
                .host(host).namespace(namespace).serviceName(serviceName).port(port)
                .resultType(resultType).scheme(scheme);
    }


    @Override
    public String getName() {
        return serviceName;
    }

    @Override
    public String host(String namespace) {
        return host;
    }

    @Override
    public T supply(NetSocketAddress address) {
        if (resultType.isAssignableFrom(NetSocketAddress.class)) {
            return resultType.cast(address);
        } else if (resultType.isAssignableFrom(URL.class)) {
            return resultType.cast(address.toUrl(scheme));
        } else {
            throw new IllegalArgumentException("Unsupported result type: " + resultType.getName());
        }
    }
}
