# Spec Delta

## Purpose

Lets service developers explicitly mark gateway paths as forbidden on specific border gateways, replacing legacy `allowed: false` routes. The plugin enforces these with Istio `AuthorizationPolicy` DENY rules, which are evaluated before route selection and so do not depend on route ordering.

## ADDED Requirements

### Requirement: ForbiddenRoute annotation
The route-registration-common library SHALL provide the annotation `com.netcracker.cloud.routesregistration.common.annotation.ForbiddenRoute`:

- It has `RUNTIME` retention and targets types and methods.
- Its `value` attribute is a mandatory array of `RouteType`.
- Each value names one border gateway: `PUBLIC` → public gateway, `PRIVATE` → private gateway, `INTERNAL` → internal gateway. A value does not imply the wider gateways.

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
- **WHEN** a class mapped to `/api/v1/svc/order` has `@ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE, RouteType.INTERNAL})` and no class-level `@Route`, and one of its methods has `@Route(RouteType.PUBLIC)` on `/{id}/items`
- **THEN** `/api/v1/svc/order` is forbidden on all three border gateways, and `/api/v1/svc/order/{id}/items` stays allowed on all three

#### Scenario: Gateway path remapping
- **WHEN** a method has `@ForbiddenRoute(RouteType.PUBLIC)` and a gateway mapping `@GatewayRequestMapping("/api/x/{id}/secret")`
- **THEN** the forbidden path is `/api/x/{id}/secret`, not the service path

### Requirement: Invalid ForbiddenRoute usage
The plugin SHALL fail the build with an error naming the annotated element when a `@ForbiddenRoute`:

- has an empty `value` or contains `FACADE`;
- resolves to a gateway path where a variable does not take up a whole path segment (for example `/files/{name}.txt`);
- resolves to a gateway path that contains `*` wildcards or a regex-constrained variable (for example `{id:\d+}`).

#### Scenario: Facade is not supported
- **WHEN** a method has `@ForbiddenRoute(RouteType.FACADE)`
- **THEN** the build fails with an error stating that `@ForbiddenRoute` supports only PUBLIC, PRIVATE and INTERNAL

#### Scenario: Partial-segment variable
- **WHEN** a method has `@ForbiddenRoute(RouteType.PUBLIC)` with gateway path `/files/{name}.txt`
- **THEN** the build fails with an error stating that forbidden paths need whole-segment variables

### Requirement: AuthorizationPolicy generation
For each border gateway that has at least one forbidden path, the plugin SHALL generate exactly one `security.istio.io/v1` `AuthorizationPolicy` with `action: DENY`:

- `metadata.name` is `{{ .Values.SERVICE_NAME }}-java-annotations-deny-<gateway>`, where `<gateway>` is `public`, `private` or `internal`.
- The metadata labels are the same as for generated HTTPRoutes (defaults or the configured `labels`).
- `targetRefs` has one entry: `group: gateway.networking.k8s.io`, `kind: Gateway`, `name: public-gateway` for public; the same with `name: private-gateway` for private; `group: ""`, `kind: Service`, `name: internal-gateway-service` for internal.

When `autoGenerateAuthorizationPolicies` is `false` (the default), the plugin SHALL NOT generate any AuthorizationPolicy when no `@ForbiddenRoute` is declared. It SHALL NOT generate DENY rules for legacy implicit forbidden routes or for exposure caused by the cut; the validation step reports those instead.

#### Scenario: No forbidden declarations
- **WHEN** the project has no `@ForbiddenRoute`, and `autoGenerateAuthorizationPolicies` is not configured
- **THEN** the output contains no `AuthorizationPolicy`

#### Scenario: Forbidden on public and private
- **WHEN** one method has `@ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE})`
- **THEN** the output contains two AuthorizationPolicies, targeting Gateway `public-gateway` and Gateway `private-gateway`, and none targeting `internal-gateway-service`

### Requirement: DENY rule content
Each forbidden path F on a gateway SHALL produce one rule in that gateway's policy:

- `to[].operation.paths` contains F and F + `/{**}`, with every path variable replaced by `{*}`.
- `to[].operation.notPaths` contains Q and Q + `/{**}` (same variable replacement) for every route Q that is allowed on the same gateway in the legacy model, overlaps F, and has a longer gateway path than F. These are the routes that won over F in legacy. Q overlaps F when their segments agree up to the shorter of the two: a variable overlaps any segment it can match, including a literal of the other path, and two literals must be equal.
- A trailing `/` of F or Q SHALL be dropped.
- `notPaths` SHALL be omitted when there are none.
- `to[].operation.ports` is set from the ports that the plugin parameter `authorizationPolicyPorts` configures for that gateway.

Paths within a rule SHALL be deduplicated and sorted deterministically. A forbidden root path `/` SHALL produce `paths: ["/", "/{**}"]`.

#### Scenario: Forbidden route with a longer allowed route below it
- **WHEN** PUBLIC route `/api/v1/svc/resource`, INTERNAL route `/api/v1/svc/resource/{var1}/internal-api` with `@ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE})`, and PUBLIC route `/api/v1/svc/resource/{var1}/internal-api/status` are declared
- **THEN** the public gateway policy has a rule with `paths` `/api/v1/svc/resource/{*}/internal-api` and `/api/v1/svc/resource/{*}/internal-api/{**}`, and `notPaths` `/api/v1/svc/resource/{*}/internal-api/status` and `/api/v1/svc/resource/{*}/internal-api/status/{**}`

#### Scenario: Forbidden controller root to close an exposure caused by the cut
- **WHEN** a class mapped to `/api/v1/svc/order` has `@ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE, RouteType.INTERNAL})`, and PUBLIC method route `/api/v1/svc/order/{id}/items` is declared
- **THEN** each of the three policies has a rule with `paths` `/api/v1/svc/order` and `/api/v1/svc/order/{**}`, and `notPaths` `/api/v1/svc/order/{*}/items` and `/api/v1/svc/order/{*}/items/{**}`, and validation reports no Exposure for `/api/v1/svc/order/<sample>`

#### Scenario: Longer allowed route with a variable where the forbidden path has a literal
- **WHEN** `@ForbiddenRoute(RouteType.PUBLIC)` forbids `/a/lit/x`, and PUBLIC route `/a/{id}/x/y` is declared
- **THEN** the public gateway policy has a rule with `paths` `/a/lit/x` and `/a/lit/x/{**}`, and `notPaths` `/a/{*}/x/y` and `/a/{*}/x/y/{**}`, and validation reports no Lost route for `/a/lit/x/y`

#### Scenario: Forbidden path with a trailing slash
- **WHEN** `@ForbiddenRoute(RouteType.PUBLIC)` forbids `/a/b/`
- **THEN** the rule has `paths` `/a/b` and `/a/b/{**}`

#### Scenario: Nested forbidden paths
- **WHEN** forbidden path `/a` and forbidden path `/a/b/{id}/c` exist on the same gateway, and allowed route `/a/b` is longer than `/a` and lies below it
- **THEN** the policy has two separate rules: the `/a` rule lists `/a/b` and `/a/b/{**}` in `notPaths`, and the `/a/b/{*}/c` rule has its own `paths`

### Requirement: Automatic DENY rules
The plugin SHALL accept the boolean parameter `autoGenerateAuthorizationPolicies`, set in the plugin `<configuration>` in `pom.xml` and defaulting to `false`. When it is `true`, the plugin SHALL add DENY rules to the border gateway policies, besides those from `@ForbiddenRoute`:

- For every legacy forbidden entry F that comes from a route type (a narrower-type route on a wider gateway), where the generated HTTPRoutes attached to that gateway would route F: a rule for F, built as in "DENY rule content". F SHALL be skipped when `@ForbiddenRoute` would reject its gateway path (for example a partial-segment variable), because `{*}` would deny the whole segment; the validation step reports the exposure instead.
- For every generated `PathPrefix` value P that comes from cutting a gateway path with variables, where the legacy model does not route P itself on that gateway: a rule with `paths` P and P + `/{**}`, and `notPaths` Q and Q + `/{**}` for every allowed route Q on that gateway that lies below P and is longer than P.

Whether Istio routes a path SHALL be decided from the generated HTTPRoute rules alone, ignoring all DENY rules. Automatic rules SHALL NOT be generated for the service-bound HTTPRoute of facade and composite routes. A rule that is identical to a rule from `@ForbiddenRoute` (same gateway, `paths` and `notPaths`) SHALL be rendered once. Route migration validation SHALL run on all generated rules, automatic ones included. Whether a cut exposure rule is needed SHALL be decided on P alone, so an allowed route below P that is not longer than P (for example a catch-all from another class) is not in `notPaths`; validation reports the requests it loses as Lost route.

#### Scenario: Parameter not set
- **WHEN** `autoGenerateAuthorizationPolicies` is not configured, and PUBLIC route `/api/v1/svc/resource` and INTERNAL route `/api/v1/svc/resource/{id}/internal-api` are declared without `@ForbiddenRoute`
- **THEN** no AuthorizationPolicy is generated, and the build fails with Exposure errors

#### Scenario: Implicit forbidden route with the parameter enabled
- **WHEN** `autoGenerateAuthorizationPolicies` is `true`, and PUBLIC route `/api/v1/svc/resource`, INTERNAL route `/api/v1/svc/resource/{id}/internal-api` and PUBLIC route `/api/v1/svc/resource/{id}/internal-api/status` are declared without `@ForbiddenRoute`
- **THEN** the public and private gateway policies each have a rule with `paths` `/api/v1/svc/resource/{*}/internal-api` and `/api/v1/svc/resource/{*}/internal-api/{**}`, and `notPaths` `/api/v1/svc/resource/{*}/internal-api/status` and `/api/v1/svc/resource/{*}/internal-api/status/{**}`. No internal gateway policy is generated, and validation reports no error

#### Scenario: Cut exposure with the parameter enabled
- **WHEN** `autoGenerateAuthorizationPolicies` is `true`, and only PUBLIC route `/api/v1/svc/order/{id}/items` is declared under `/api/v1/svc/order`
- **THEN** each of the three border gateway policies has a rule with `paths` `/api/v1/svc/order` and `/api/v1/svc/order/{**}`, and `notPaths` `/api/v1/svc/order/{*}/items` and `/api/v1/svc/order/{*}/items/{**}`, and validation reports no error

#### Scenario: No rule when legacy already exposed the subtree
- **WHEN** `autoGenerateAuthorizationPolicies` is `true`, and PUBLIC routes `/api/v1/svc/order` and `/api/v1/svc/order/{id}/items` are declared with the same rewrite
- **THEN** no DENY rule is generated for `/api/v1/svc/order`

#### Scenario: No rule for a partial-segment variable
- **WHEN** `autoGenerateAuthorizationPolicies` is `true`, and PUBLIC route `/api/files` and PRIVATE route `/api/files/{name}.txt` are declared
- **THEN** no DENY rule is generated, and the build fails with an Exposure error on the public gateway

#### Scenario: Automatic rule over a shorter catch-all route
- **WHEN** `autoGenerateAuthorizationPolicies` is `true`, and PUBLIC routes `/api/v1/my-service/order/{id}/items` and `/{a}/{b}/{c}/{d}/{e}` are declared
- **THEN** the cut exposure rule for `/api/v1/my-service/order` is generated, and the build fails with a Lost route error for `/api/v1/my-service/order/<sample>`, which legacy routes by the catch-all route

#### Scenario: No rule when Istio doesn't route the forbidden path
- **WHEN** `autoGenerateAuthorizationPolicies` is `true`, and only INTERNAL route `/api/v1/svc/admin` is declared
- **THEN** no AuthorizationPolicy is generated for the public or private gateway

#### Scenario: Explicit and automatic rule are the same
- **WHEN** `autoGenerateAuthorizationPolicies` is `true`, and the internal-api method also has `@ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE})`
- **THEN** each of the public and private policies contains that rule once

### Requirement: Port scoping parameter
The plugin SHALL accept the optional parameter `authorizationPolicyPorts`, a map from border gateway name (`public-gateway-service`, `private-gateway-service`, `internal-gateway-service`) to a comma-separated list of ports. A gateway that is not listed SHALL use port `8080`, and when the parameter is not configured, every gateway SHALL use `8080`. The build SHALL NOT fail or warn because the parameter or an entry is missing. Every DENY rule of a gateway's policy SHALL be scoped to that gateway's ports, so that non-HTTP traffic is not denied unintentionally.

The build SHALL fail with a configuration error, before any file is written, when a key is not a border gateway name, a value is empty, or a port is not a number from 1 to 65535.

#### Scenario: Default ports
- **WHEN** `authorizationPolicyPorts` is not configured
- **THEN** every DENY rule operation of every policy has `ports: ["8080"]`

#### Scenario: Ports for one gateway
- **WHEN** `authorizationPolicyPorts` has only `<internal-gateway-service>8080,8443</internal-gateway-service>`, and policies are generated for all three border gateways
- **THEN** every DENY rule of the internal gateway policy has `ports: ["8080", "8443"]`, and every DENY rule of the public and private gateway policies has `ports: ["8080"]`

#### Scenario: Unknown gateway name
- **WHEN** `authorizationPolicyPorts` has an entry `<facade-gateway>8080</facade-gateway>`
- **THEN** the build fails with an error naming `facade-gateway` and listing the allowed gateway names, and the output file is not written

### Requirement: Policies share the Helm guard and header
Generated AuthorizationPolicies SHALL be written to the same output file as HTTPRoutes, after all HTTPRoute resources. They SHALL be inside the same `SERVICE_MESH_TYPE == Istio` Helm guard and below the same generated-file header.

#### Scenario: Combined output
- **WHEN** routes and forbidden routes are declared
- **THEN** the output file has the header, then the Istio guard, then the HTTPRoute documents, then the AuthorizationPolicy documents, then the guard's end
