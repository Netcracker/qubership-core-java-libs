# Spec Delta

## Purpose

Lets service developers explicitly mark gateway paths as forbidden on the public and/or private gateway, replacing legacy `allowed: false` routes. The plugin enforces these with Istio `AuthorizationPolicy` DENY rules, which are evaluated before route selection and so do not depend on route ordering.

## ADDED Requirements

### Requirement: ForbiddenRoute annotation
The route-registration-common library SHALL provide the annotation `com.netcracker.cloud.routesregistration.common.annotation.ForbiddenRoute`:

- It has `RUNTIME` retention and targets types and methods.
- Its `value` attribute is a mandatory array of `RouteType`.
- Each value names one external gateway: `PUBLIC` → public gateway, `PRIVATE` → private gateway. A value does not imply the other gateway. `INTERNAL` and `FACADE` are not supported.

The legacy runtime route registration SHALL NOT change its behavior because of this annotation.

#### Scenario: Annotating an internal-only method
- **WHEN** a method has `@Route(RouteType.INTERNAL)` and `@ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE})`
- **THEN** the method's gateway path is treated as forbidden on the public and private gateways and allowed on the internal gateway

#### Scenario: Legacy runtime unaffected
- **WHEN** a service using the legacy mesh registers routes at runtime from classes annotated with `@ForbiddenRoute`
- **THEN** the routes it registers are the same as without the annotation

### Requirement: Forbidden gateway path resolution
The plugin SHALL resolve the forbidden gateway paths of an annotated class or method the same way it resolves gateway paths for `@Route`: from class and method request mappings, `@Gateway` and `@GatewayRequestMapping`. A class-level `@ForbiddenRoute` SHALL forbid only the class-level gateway path(s), not each method path separately. `@ForbiddenRoute` SHALL be scanned on classes and methods even when they carry no `@Route`.

#### Scenario: Class-level forbidden root without a Route
- **WHEN** a class mapped to `/api/v1/svc/order` has `@ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE})` and no class-level `@Route`, and one of its methods has `@Route(RouteType.PUBLIC)` on `/{id}/items`
- **THEN** `/api/v1/svc/order` is forbidden on the public and private gateways, and `/api/v1/svc/order/{id}/items` stays allowed on both

#### Scenario: Gateway path remapping
- **WHEN** a method has `@ForbiddenRoute(RouteType.PUBLIC)` and a gateway mapping `@GatewayRequestMapping("/api/x/{id}/secret")`
- **THEN** the forbidden path is `/api/x/{id}/secret`, not the service path

### Requirement: Invalid ForbiddenRoute usage
The plugin SHALL fail the build with an error naming the annotated element when a `@ForbiddenRoute` has an empty `value` or contains anything but `PUBLIC` or `PRIVATE`. It SHALL fail the build with an error naming the path when a DENY rule path, or a route in its `notPaths`, has a variable that does not take up a whole path segment (for example `/files/{name}.txt`), because an Istio path template can't express it.

#### Scenario: Facade is not supported
- **WHEN** a method has `@ForbiddenRoute(RouteType.FACADE)`
- **THEN** the build fails with an error stating that `@ForbiddenRoute` must list PUBLIC and/or PRIVATE

#### Scenario: Internal is not supported
- **WHEN** a method has `@ForbiddenRoute({RouteType.PUBLIC, RouteType.INTERNAL})`
- **THEN** the build fails with an error stating that `@ForbiddenRoute` must list PUBLIC and/or PRIVATE, and the method's path is not forbidden on any gateway

#### Scenario: Partial-segment variable
- **WHEN** a method has `@ForbiddenRoute(RouteType.PUBLIC)` with gateway path `/files/{name}.txt`
- **THEN** the build fails with an error stating that variables must take up whole path segments

### Requirement: AuthorizationPolicy generation
For each of the public and private gateways that has at least one forbidden path, the plugin SHALL generate exactly one `security.istio.io/v1` `AuthorizationPolicy` with `action: DENY`:

- `metadata.name` is `{{ .Values.SERVICE_NAME }}-java-annotations-deny-<gateway>`, where `<gateway>` is `public` or `private`. No policy is generated for the internal gateway.
- The metadata labels are the same as for generated HTTPRoutes (defaults or the configured `labels`).
- `targetRefs` has one entry: `group: gateway.networking.k8s.io`, `kind: Gateway`, `name: public-gateway` for public; the same with `name: private-gateway` for private.

When `autoGenerateAuthorizationPolicies` is `false` (the default), the plugin SHALL NOT generate any AuthorizationPolicy when no `@ForbiddenRoute` is declared. It SHALL NOT generate DENY rules for legacy implicit forbidden routes or for exposure caused by the cut; the build fails with errors for them instead.

#### Scenario: No forbidden declarations
- **WHEN** the project has no `@ForbiddenRoute`, and `autoGenerateAuthorizationPolicies` is not configured
- **THEN** the output contains no `AuthorizationPolicy`

#### Scenario: Forbidden on public and private
- **WHEN** one method has `@ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE})`
- **THEN** the output contains two AuthorizationPolicies, targeting Gateway `public-gateway` and Gateway `private-gateway`, and none targeting `internal-gateway-service`

### Requirement: DENY rule content
Each forbidden path F on a gateway SHALL produce one rule in that gateway's policy:

- `to[].operation.paths` contains F and F + `/{**}`, with every path variable replaced by `{*}`.
- `to[].operation.notPaths` contains Q and Q + `/{**}` (same variable replacement) for every route Q that is allowed on the same gateway in the legacy model, overlaps F, and has a longer gateway path than F. These are the routes that won over F in legacy. It SHALL also contain every other route allowed on the gateway that legacy routes a request of F by: the plugin probes F and its intersection with every allowed route that overlaps it, and excludes the legacy route of each probe the rule would deny, so that the rule denies nothing legacy routed. Between an allowed and a forbidden route of the same length, the legacy route of a probe SHALL be the forbidden one, so the allowed route is not excluded (user decision). For F from `@ForbiddenRoute`, the routes that cover F itself SHALL NOT be excluded: an explicit rule denies its path even where legacy routed it by a shorter allowed route. Q overlaps F when their segments agree up to the shorter of the two: a variable overlaps any segment it can match, including a literal of the other path, and two literals must be equal.
- A trailing `/` of F or Q SHALL be dropped.
- `notPaths` SHALL be omitted when there are none.
- `to[].operation.ports` is `["8080"]`, the listener port of the public and private gateways.

Paths within a rule SHALL be deduplicated and sorted deterministically. A forbidden root path `/` SHALL produce `paths: ["/", "/{**}"]`.

#### Scenario: Forbidden route with a longer allowed route below it
- **WHEN** PUBLIC route `/api/v1/svc/resource`, INTERNAL route `/api/v1/svc/resource/{var1}/internal-api` with `@ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE})`, and PUBLIC route `/api/v1/svc/resource/{var1}/internal-api/status` are declared
- **THEN** the public gateway policy has a rule with `paths` `/api/v1/svc/resource/{*}/internal-api` and `/api/v1/svc/resource/{*}/internal-api/{**}`, and `notPaths` `/api/v1/svc/resource/{*}/internal-api/status` and `/api/v1/svc/resource/{*}/internal-api/status/{**}`

#### Scenario: Forbidden controller root to close an exposure caused by the cut
- **WHEN** a class mapped to `/api/v1/svc/order` has `@ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE})`, and PUBLIC method route `/api/v1/svc/order/{id}/items` is declared
- **THEN** each of the two policies has a rule with `paths` `/api/v1/svc/order` and `/api/v1/svc/order/{**}`, and `notPaths` `/api/v1/svc/order/{*}/items` and `/api/v1/svc/order/{*}/items/{**}`, and no error is reported

#### Scenario: Longer allowed route with a variable where the forbidden path has a literal
- **WHEN** `@ForbiddenRoute(RouteType.PUBLIC)` forbids `/a/lit/x`, and PUBLIC route `/a/{id}/x/y` is declared
- **THEN** the public gateway policy has a rule with `paths` `/a/lit/x` and `/a/lit/x/{**}`, and `notPaths` `/a/{*}/x/y` and `/a/{*}/x/y/{**}`, so `/a/lit/x/y` stays routed

#### Scenario: Shorter allowed route that legacy routes the forbidden path by
- **WHEN** PUBLIC route `/api/v1/svc` is declared, and `@ForbiddenRoute(RouteType.PUBLIC)` forbids `/api/v1/svc/admin`
- **THEN** the public gateway policy has a rule with `paths` `/api/v1/svc/admin` and `/api/v1/svc/admin/{**}` and no `notPaths`, so `/api/v1/svc/admin` is denied and the rest of `/api/v1/svc` stays routed

#### Scenario: Deeper but shorter allowed route
- **WHEN** PUBLIC route `/api/v1/x/y` is declared, and `@ForbiddenRoute(RouteType.PUBLIC)` forbids `/api/{version}`
- **THEN** the public gateway policy has a rule with `paths` `/api/{*}` and `/api/{*}/{**}`, and `notPaths` `/api/v1/x/y` and `/api/v1/x/y/{**}`

#### Scenario: Allowed route of the same length as the forbidden path
- **WHEN** `autoGenerateAuthorizationPolicies` is `true`, and PUBLIC routes `/api/v1/svc/r` and `/api/v1/svc/r/list` and INTERNAL route `/api/v1/svc/r/{id}` are declared
- **THEN** the public and private gateway policies each have a rule with `paths` `/api/v1/svc/r/{*}` and `/api/v1/svc/r/{*}/{**}` and no `notPaths`, so `/api/v1/svc/r/list` is denied

#### Scenario: Allowed route longer than the forbidden path it overlaps
- **WHEN** `autoGenerateAuthorizationPolicies` is `true`, and PUBLIC routes `/api/v1/svc/r` and `/api/v1/svc/r/list` and INTERNAL route `/api/v1/svc/r/{i}` are declared
- **THEN** the public gateway policy has a rule with `paths` `/api/v1/svc/r/{*}` and `/api/v1/svc/r/{*}/{**}`, and `notPaths` `/api/v1/svc/r/list` and `/api/v1/svc/r/list/{**}`

#### Scenario: Forbidden path below a longer forbidden route
- **WHEN** PUBLIC route `/api/v1/svc` and PRIVATE route `/api/v1/svc/admin` are declared, and `@ForbiddenRoute(RouteType.PUBLIC)` forbids `/api/v1/svc/admin` and `/api/v1/svc/admin/users`
- **THEN** neither rule lists `/api/v1/svc` in `notPaths`, because legacy forbids both paths on the public gateway by `/api/v1/svc/admin`

#### Scenario: Forbidden path with a trailing slash
- **WHEN** `@ForbiddenRoute(RouteType.PUBLIC)` forbids `/a/b/`
- **THEN** the rule has `paths` `/a/b` and `/a/b/{**}`

#### Scenario: Nested forbidden paths
- **WHEN** forbidden path `/a` and forbidden path `/a/b/{id}/c` exist on the same gateway, and allowed route `/a/b` is longer than `/a` and lies below it
- **THEN** the policy has two separate rules: the `/a` rule lists `/a/b` and `/a/b/{**}` in `notPaths`, and the `/a/b/{*}/c` rule has its own `paths`

### Requirement: Automatic DENY rules
The plugin SHALL accept the boolean parameter `autoGenerateAuthorizationPolicies`, set in the plugin `<configuration>` in `pom.xml` and defaulting to `false`. When it is `true`, the plugin SHALL add a DENY rule, built as in "DENY rule content", for every path that needs one (see route-migration-validation, "Paths that need DENY rules"), besides those from `@ForbiddenRoute`. Automatic rules SHALL NOT be generated for the service-bound HTTPRoute of facade and composite routes. A rule with the same path as a rule from `@ForbiddenRoute` on the same gateway SHALL be rendered once.

#### Scenario: Parameter not set
- **WHEN** `autoGenerateAuthorizationPolicies` is not configured, and PUBLIC route `/api/v1/svc/resource` and INTERNAL route `/api/v1/svc/resource/{id}/internal-api` are declared without `@ForbiddenRoute`
- **THEN** no AuthorizationPolicy is generated, and the build fails with an error that suggests `@ForbiddenRoute({PUBLIC, PRIVATE})` or the parameter

#### Scenario: Implicit forbidden route with the parameter enabled
- **WHEN** `autoGenerateAuthorizationPolicies` is `true`, and PUBLIC route `/api/v1/svc/resource`, INTERNAL route `/api/v1/svc/resource/{id}/internal-api` and PUBLIC route `/api/v1/svc/resource/{id}/internal-api/status` are declared without `@ForbiddenRoute`
- **THEN** the public and private gateway policies each have a rule with `paths` `/api/v1/svc/resource/{*}/internal-api` and `/api/v1/svc/resource/{*}/internal-api/{**}`, and `notPaths` `/api/v1/svc/resource/{*}/internal-api/status` and `/api/v1/svc/resource/{*}/internal-api/status/{**}`. No internal gateway policy is generated, and no error is reported

#### Scenario: Cut exposure with the parameter enabled
- **WHEN** `autoGenerateAuthorizationPolicies` is `true`, and only PUBLIC route `/api/v1/svc/order/{id}/items` is declared under `/api/v1/svc/order`
- **THEN** the public and private gateway policies each have a rule with `paths` `/api/v1/svc/order` and `/api/v1/svc/order/{**}`, and `notPaths` `/api/v1/svc/order/{*}/items` and `/api/v1/svc/order/{*}/items/{**}`, and no error is reported

#### Scenario: Cut exposure of a prefix that other services share
- **WHEN** `autoGenerateAuthorizationPolicies` is `true`, and only PUBLIC route `/api/{version}/svc/items` is declared
- **THEN** the public and private gateway policies each have a rule with `paths` `/api` and `/api/{**}`, and `notPaths` `/api/{*}/svc/items` and `/api/{*}/svc/items/{**}`, so other services' paths under `/api` are denied too, and no error is reported

#### Scenario: No rule when legacy already exposed the subtree
- **WHEN** `autoGenerateAuthorizationPolicies` is `true`, and PUBLIC routes `/api/v1/svc/order` and `/api/v1/svc/order/{id}/items` are declared with the same rewrite
- **THEN** no DENY rule is generated for `/api/v1/svc/order`

#### Scenario: No rule for a partial-segment variable
- **WHEN** `autoGenerateAuthorizationPolicies` is `true`, and PUBLIC route `/api/files` and PRIVATE route `/api/files/{name}.txt` are declared
- **THEN** no DENY rule is generated for `/api/files/{name}.txt`, and the build fails with an error that it can't be expressed

#### Scenario: No rule when Istio doesn't route the forbidden path
- **WHEN** `autoGenerateAuthorizationPolicies` is `true`, and only INTERNAL route `/api/v1/svc/admin` is declared
- **THEN** no AuthorizationPolicy is generated for the public or private gateway

#### Scenario: Explicit and automatic rule are the same
- **WHEN** `autoGenerateAuthorizationPolicies` is `true`, and the internal-api method also has `@ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE})`
- **THEN** each of the public and private policies contains that rule once

### Requirement: Policies share the Helm guard and header
Generated AuthorizationPolicies SHALL be written to the same output file as HTTPRoutes, after all HTTPRoute resources. They SHALL be inside the same `SERVICE_MESH_TYPE == Istio` Helm guard and below the same generated-file header.

#### Scenario: Combined output
- **WHEN** routes and forbidden routes are declared
- **THEN** the output file has the header, then the Istio guard, then the HTTPRoute documents, then the AuthorizationPolicy documents, then the guard's end
