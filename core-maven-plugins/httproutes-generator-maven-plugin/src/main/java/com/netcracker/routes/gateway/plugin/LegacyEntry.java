package com.netcracker.routes.gateway.plugin;

import java.util.Objects;

/**
 * One route registered on a legacy border gateway: allowed, or forbidden ({@code allowed: false}, 404).
 *
 * @param pattern     gateway path
 * @param kind        how the entry came about
 * @param route       the declared route, for {@link Kind#ALLOWED} and {@link Kind#IMPLICIT_FORBIDDEN}
 * @param declaration the {@code @ForbiddenRoute} declaration, for {@link Kind#EXPLICIT_FORBIDDEN}
 */
public record LegacyEntry(PathPattern pattern, Kind kind, DeclaredRoute route, ForbiddenDeclaration declaration) {

    public LegacyEntry {
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(kind, "kind");
        if ((kind == Kind.EXPLICIT_FORBIDDEN) != (declaration != null) || (kind == Kind.EXPLICIT_FORBIDDEN) == (route != null)) {
            throw new IllegalArgumentException("explicit forbidden entries need a declaration, other entries a route");
        }
    }

    public static LegacyEntry allowed(DeclaredRoute route) {
        return new LegacyEntry(PathPattern.of(route.route().gatewayPath()), Kind.ALLOWED, route, null);
    }

    public static LegacyEntry implicitForbidden(DeclaredRoute route) {
        return new LegacyEntry(PathPattern.of(route.route().gatewayPath()), Kind.IMPLICIT_FORBIDDEN, route, null);
    }

    public static LegacyEntry explicitForbidden(ForbiddenDeclaration declaration) {
        return new LegacyEntry(PathPattern.of(declaration.gatewayPath()), Kind.EXPLICIT_FORBIDDEN, null, declaration);
    }

    public boolean isAllowed() {
        return kind == Kind.ALLOWED;
    }

    public long timeout() {
        return route == null ? 0 : route.route().timeout();
    }

    /**
     * Legacy regex rewrite: the captured variables replace the service path variables by position,
     * and the rest of the request path is appended unchanged.
     */
    public String upstream(PathPattern.Match match) {
        String servicePath = PathPattern.of(route.route().path()).substitute(match.captures());
        return PathPattern.appendRemainder(servicePath, match.remainder());
    }

    public String origin() {
        return kind == Kind.EXPLICIT_FORBIDDEN ? declaration.origin() : route.originsText();
    }

    /**
     * @return for example {@code FORBIDDEN (implicit: INTERNAL route /a/{id} -> /x/{id} from com.acme.C#m)}
     */
    public String describe() {
        return switch (kind) {
            case ALLOWED -> "ROUTED by " + route.describe();
            case IMPLICIT_FORBIDDEN -> "FORBIDDEN (implicit: " + route.describe() + ")";
            case EXPLICIT_FORBIDDEN -> "FORBIDDEN (@ForbiddenRoute " + declaration.gatewayPath() + " on " + declaration.origin() + ")";
        };
    }

    public enum Kind {
        /** a route whose type exposes the gateway */
        ALLOWED,
        /** a route of a narrower type, registered with {@code allowed: false} on a wider gateway */
        IMPLICIT_FORBIDDEN,
        /** a {@code @ForbiddenRoute} declaration */
        EXPLICIT_FORBIDDEN
    }
}
