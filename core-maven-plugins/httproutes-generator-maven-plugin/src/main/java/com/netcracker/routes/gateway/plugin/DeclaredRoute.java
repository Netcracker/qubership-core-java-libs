package com.netcracker.routes.gateway.plugin;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A scanned route together with the elements that declared it. Each origin is {@code class#method} or {@code class}.
 */
public record DeclaredRoute(HttpRoute route, SortedSet<String> origins) {

    public DeclaredRoute {
        Objects.requireNonNull(route, "route");
        origins = Collections.unmodifiableSortedSet(new TreeSet<>(origins));
    }

    public String originsText() {
        return String.join(", ", origins);
    }

    public String describe() {
        HttpRoute r = route;
        String target = r.gatewayPath().equals(r.path()) ? r.gatewayPath() : r.gatewayPath() + " -> " + r.path();
        return r.type() + " route " + target + " (from " + originsText() + ")";
    }
}
