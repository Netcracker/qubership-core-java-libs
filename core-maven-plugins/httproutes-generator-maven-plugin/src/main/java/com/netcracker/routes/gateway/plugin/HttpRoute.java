package com.netcracker.routes.gateway.plugin;

import java.util.Objects;

public record HttpRoute(String path, String gatewayPath, Type type, long timeout) {

    public HttpRoute(String path, String gatewayPath, Type type, long timeout) {
        this.path = normalizePath(path);
        this.gatewayPath = normalizePath(gatewayPath);
        this.type = Objects.requireNonNull(type, "type");
        this.timeout = timeout;
    }

    public HttpRoute(String path, Type type, long timeout) {
        this(path, path, type, timeout);
    }

    public HttpRoute(String path, Type type) {
        this(path, path, type);
    }

    public HttpRoute(String path, String gatewayPath, Type type) {
        this(path, gatewayPath, type, 0);
    }

    public enum Type {
        INTERNAL("internal-gateway-service"),
        PRIVATE("private-gateway"),
        PUBLIC("public-gateway"),
        FACADE("facade");

        private final String gatewayName;

        public String gatewayName() {
            return gatewayName;
        }

        Type(String gatewayName) {
            this.gatewayName = gatewayName;
        }
    }

    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        if (!path.startsWith("/")) {
            return "/" + path;
        }
        return path;
    }
}
