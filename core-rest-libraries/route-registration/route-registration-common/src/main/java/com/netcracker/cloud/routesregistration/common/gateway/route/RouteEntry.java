package com.netcracker.cloud.routesregistration.common.gateway.route;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.*;

import java.util.Objects;
import java.util.Set;

/**
 * {@code RouteEntry} represents granular route to be registered.
 * {@code RouteEntry} contract is universal and not bound to any
 * control-plane REST API version.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RouteEntry {

    @NotNull(message = "Route can't have a null path")
    private String from;

    @NotNull(message = "Route can't have a null path")
    private String to;

    @Null(message = "Route type can be null if Gateways specified")
    private RouteType type;

    @Null(message = "Allowed can be null: default value is true")
    private boolean allowed = true;

    @NotNull(message = "Namespace shouldn't be null")
    private String namespace = "default";

    @Null(message = "Route timeout can be null")
    private Long timeout;

    @Null(message = "Gateway can be null if Route type specified")
    private String gateway;

    @Null
    private Set<String> hosts;

    public RouteEntry(RouteEntry routeEntry) {
        this.from = routeEntry.from;
        this.to = routeEntry.to;
        this.type = routeEntry.type;
        this.allowed = routeEntry.allowed;
        this.namespace = routeEntry.namespace;
        this.timeout = routeEntry.timeout;
        this.gateway = routeEntry.gateway;
        this.hosts = routeEntry.hosts;
    }

    public RouteEntry(String path, RouteType type) {
        this.from = path;
        this.to = path;
        this.type = type;
        namespace = "default";
    }

    public RouteEntry(String path, RouteType type, Long timeout) {
        this(path, type);
        this.timeout = timeout;
    }

    public RouteEntry(String path, RouteType type, String namespace) {
        this(path, type);
        this.namespace = namespace;
    }

    public RouteEntry(String path, RouteType type, String namespace, Long timeout) {
        this(path, type, namespace);
        this.timeout = timeout;
    }

    public RouteEntry(String from, String to, RouteType type) {
        this.from = from;
        this.to = to;
        this.type = type;
        namespace = "default";
    }

    public RouteEntry(String from, String to, RouteType type, Long timeout) {
        this(from, to, type);
        this.timeout = timeout;
    }

    public RouteEntry(String from, String to, RouteType type, String namespace) {
        this(from, to, type);
        this.namespace = namespace;
    }

    public RouteEntry(String from, String to, RouteType type, String namespace, Long timeout) {
        this(from, to, type, namespace);
        this.timeout = timeout;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteEntry that = (RouteEntry) o;
        return from.equals(that.from) && sameRouteTypes(that) && sameHosts(that);
    }

    private boolean sameRouteTypes(RouteEntry that) {
        if (gateway == null || gateway.isEmpty()) {
            if (that.gateway != null && !that.gateway.isEmpty()) {
                return false;
            }
            return type == that.type;
        }
        return gateway.equals(that.gateway);
    }

    private boolean sameHosts(RouteEntry that) {
        return Objects.equals(hosts, that.hosts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, gateway);
    }

    public static class RouteEntryBuilder {
        private boolean allowed = true;
        private String namespace = "default";
    }
}
