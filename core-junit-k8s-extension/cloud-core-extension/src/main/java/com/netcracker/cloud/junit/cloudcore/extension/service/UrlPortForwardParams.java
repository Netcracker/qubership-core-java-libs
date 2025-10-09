package com.netcracker.cloud.junit.cloudcore.extension.service;

import lombok.Getter;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class UrlPortForwardParams implements BasePortForwardParams {
    private final String serviceName;
    private final int port;
    private final String namespace;
    private final String host;

    public static final Pattern urlPattern = Pattern.compile("^(\\w+://)?(?<host>[^:/]+)(:(?<port>\\d+))?$");
    public static final Pattern hostPattern = Pattern.compile("^(?<service>[^.\\s]+)(\\.(?<namespace>[^.\\s]+)(\\..+)*)?$");

    public UrlPortForwardParams(URI uri) {
        this(uri.getHost() + ":" + uri.getPort());
    }

    public UrlPortForwardParams(String url) {
        Matcher matcher = urlPattern.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid url for port-forward: " + url + ". Must match pattern: " + urlPattern.pattern());
        }
        this.host = matcher.group("host");
        this.port = Optional.ofNullable(matcher.group("port")).map(Integer::valueOf).orElse(80);

        matcher = hostPattern.matcher(host);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid host for port-forward: " + host + ". Must match pattern: " + hostPattern.pattern());
        }
        this.namespace = matcher.group("namespace");
        this.serviceName = matcher.group("service");
    }

    @Override
    public String getName() {
        return serviceName;
    }

    @Override
    public String host() {
        return host;
    }
}
