package com.netcracker.routes.gateway.plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Border gateway of the legacy mesh together with the Istio resource that replaces it.
 * The declaration order (public, private, internal) is the order used for generated policies and reports.
 */
public enum Gateway {
    // qualified names: enum constants can't refer to the constants below by simple name
    PUBLIC("public-gateway-service", "public", HttpRoute.Type.PUBLIC,
            Gateway.PARENT_REF_GROUP_GATEWAY, Gateway.PARENT_REF_KIND_GATEWAY, "public-gateway"),
    PRIVATE("private-gateway-service", "private", HttpRoute.Type.PRIVATE,
            Gateway.PARENT_REF_GROUP_GATEWAY, Gateway.PARENT_REF_KIND_GATEWAY, "private-gateway"),
    INTERNAL("internal-gateway-service", "internal", HttpRoute.Type.INTERNAL,
            Gateway.PARENT_REF_GROUP_SERVICE, Gateway.PARENT_REF_KIND_SERVICE, "internal-gateway-service");

    public static final String PARENT_REF_KIND_SERVICE = "Service";
    public static final String PARENT_REF_KIND_GATEWAY = "Gateway";
    public static final String PARENT_REF_GROUP_GATEWAY = "gateway.networking.k8s.io";
    public static final String PARENT_REF_GROUP_SERVICE = "";

    private final String legacyName;
    private final String shortName;
    private final HttpRoute.Type routeType;
    private final String refGroup;
    private final String refKind;
    private final String refName;

    Gateway(String legacyName, String shortName, HttpRoute.Type routeType, String refGroup, String refKind, String refName) {
        this.legacyName = legacyName;
        this.shortName = shortName;
        this.routeType = routeType;
        this.refGroup = refGroup;
        this.refKind = refKind;
        this.refName = refName;
    }

    /**
     * @return gateway name used by legacy route registration, for example in {@code @Route(gateways = ...)}
     */
    public String legacyName() {
        return legacyName;
    }

    /**
     * @return short name used in generated resource names, for example {@code public}
     */
    public String shortName() {
        return shortName;
    }

    /**
     * @return route type whose routes are registered with this gateway by legacy {@code RouteType.fromGatewayName}
     */
    public HttpRoute.Type routeType() {
        return routeType;
    }

    /**
     * @return group of the Istio parentRef / targetRef
     */
    public String refGroup() {
        return refGroup;
    }

    /**
     * @return kind of the Istio parentRef / targetRef
     */
    public String refKind() {
        return refKind;
    }

    /**
     * @return name of the Istio parentRef / targetRef, also used to name the gateway in reports
     */
    public String refName() {
        return refName;
    }

    public static Optional<Gateway> fromLegacyName(String name) {
        for (Gateway gateway : values()) {
            if (gateway.legacyName.equals(name)) {
                return Optional.of(gateway);
            }
        }
        return Optional.empty();
    }

    /**
     * Maps a {@code RouteType} constant name (as found in annotations) to a gateway. {@code FACADE} maps to no gateway.
     */
    public static Optional<Gateway> fromRouteTypeName(String routeTypeName) {
        for (Gateway gateway : values()) {
            if (gateway.routeType.name().equals(routeTypeName)) {
                return Optional.of(gateway);
            }
        }
        return Optional.empty();
    }

    /**
     * Legacy exposure chain: the gateways on which a route of the given type is allowed.
     * PUBLIC → public, private, internal; PRIVATE → private, internal; INTERNAL → internal; FACADE → none.
     */
    public static Set<Gateway> exposedBy(HttpRoute.Type type) {
        return Collections.unmodifiableSet(switch (type) {
            case PUBLIC -> EnumSet.of(PUBLIC, PRIVATE, INTERNAL);
            case PRIVATE -> EnumSet.of(PRIVATE, INTERNAL);
            case INTERNAL -> EnumSet.of(INTERNAL);
            case FACADE -> EnumSet.noneOf(Gateway.class);
        });
    }

    /**
     * @return the type with the widest exposure (PUBLIC > PRIVATE > INTERNAL) among the given border types
     */
    public static HttpRoute.Type widest(Collection<HttpRoute.Type> types) {
        return types.stream()
                .max(Comparator.comparingInt(type -> exposedBy(type).size()))
                .orElseThrow();
    }
}
