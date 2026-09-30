# Spec Delta

## Purpose

Makes sure the generated Istio configuration doesn't route a request on a border gateway that the legacy Cloud-Core Service Mesh forbade or didn't route, and that routes which one Istio rule can't express fail the build. The build fails with actionable messages, so migrated services cannot silently expose endpoints or change routing.

## ADDED Requirements

### Requirement: Legacy route registration
The plugin SHALL derive the legacy behavior on each border gateway from the scanned annotations, as legacy route registration did:

- A border route of type T (PUBLIC, PRIVATE, INTERNAL) is **allowed** on the gateways that T exposes: PUBLIC → public, private, internal; PRIVATE → private, internal; INTERNAL → internal.
- The same route is **forbidden** (legacy `allowed: false`, 404) on the other border gateways, unless a route with the same gateway path is allowed there.
- A path variable matches exactly one non-empty path segment, and among the matching routes, the one with the longest gateway path string wins, with variables counted in their `{name}` source form.
- Facade and composite routes are not part of it. Istio has no facade or composite gateway, and these routes become service-bound rules.

#### Scenario: Narrower type below a wider prefix
- **WHEN** PUBLIC route `/api/v1/svc/resource` and INTERNAL route `/api/v1/svc/resource/{id}/internal-api` are declared
- **THEN** `/api/v1/svc/resource/{id}/internal-api` is forbidden on the public and private gateways and allowed on the internal gateway

### Requirement: Paths that need DENY rules
The plugin SHALL find, on each border gateway, the paths that the generated HTTPRoutes route and legacy didn't:

- the gateway path of a route forbidden on the gateway, when the cut match of a route allowed on the gateway covers it;
- the cut match of an allowed route with a path variable, when the longest route that covers the match in legacy is forbidden on the gateway, or there is none.

Each such path SHALL be covered by a DENY rule, from `@ForbiddenRoute` or, with `autoGenerateAuthorizationPolicies`, generated automatically. Otherwise the plugin SHALL report an error and fail the build.

The plugin SHALL NOT detect timeout changes between routes merged into one rule other than the merge warning, and SHALL NOT report literal-vs-variable precedence changes: where a longer route with a variable competed with a shorter literal route in legacy, the literal route wins in Istio. Routes of other services on the same gateway, and legacy routes that lead to another service or use header matchers, SHALL NOT be modeled.

#### Scenario: Implicit legacy 404 not declared
- **WHEN** PUBLIC route `/api/v1/svc/resource` and INTERNAL route `/api/v1/svc/resource/{id}/internal-api` are declared, and no `@ForbiddenRoute` covers the internal one
- **THEN** the build fails with an error for `/api/v1/svc/resource/{id}/internal-api` on the public and private gateways

#### Scenario: Implicit legacy 404 for a path without variables
- **WHEN** PUBLIC route `/api/v1/svc/resource` and INTERNAL route `/api/v1/svc/resource/admin` are declared, and no `@ForbiddenRoute` covers the internal one
- **THEN** the build fails with an error for `/api/v1/svc/resource/admin` on the public and private gateways

#### Scenario: Cut exposes controller subtree
- **WHEN** only PUBLIC route `/api/v1/svc/order/{id}/items` is declared under `/api/v1/svc/order`, and no `@ForbiddenRoute` covers it
- **THEN** the build fails with an error for `/api/v1/svc/order` on the public, private and internal gateways

#### Scenario: Cut exposure already covered by a shorter legacy route
- **WHEN** PUBLIC route `/api/v1/svc/order` and PUBLIC route `/api/v1/svc/order/{id}/items` are declared with the same rewrite
- **THEN** no error is reported and no DENY rule is generated

#### Scenario: Narrower route with no wider prefix above it
- **WHEN** PUBLIC route `/api/v1/svc/resource` and INTERNAL route `/api/v1/svc/order/internal` are declared
- **THEN** no error is reported and no DENY rule is generated

### Requirement: Contradictory declarations are errors
The plugin SHALL report an error when a `@ForbiddenRoute` declaration forbids, on some gateway, the gateway path of a route allowed on that gateway.

#### Scenario: Forbidding an allowed route's own path
- **WHEN** a method has `@Route(RouteType.PUBLIC)` and `@ForbiddenRoute(RouteType.PUBLIC)` with the same gateway path
- **THEN** the build fails with an error that names the path and the public gateway

### Requirement: Actionable failure report
Before it writes any file, the plugin SHALL log every warning and every error of the scan, HTTPRoute generation and AuthorizationPolicy generation, and then fail the build with `MojoFailureException` when there is at least one error. The exception message SHALL give the number of errors and point to the log.

- An error for a path that needs a DENY rule SHALL name the path, why it needs one, all the gateways it needs one on, the `@ForbiddenRoute(...)` gateways to add to the element mapped to it, and the alternative of setting `autoGenerateAuthorizationPolicies`.
- An error for routes that one rule can't express SHALL name every route with its gateway path, type and rewrite, and suggest aligning their gateway paths or service paths.
- An error for a path that an AuthorizationPolicy can't express SHALL name the path and say why.

#### Scenario: Missing DENY rule report content
- **WHEN** the implicit 404 scenario above fails
- **THEN** the log has one error naming `/api/v1/svc/resource/{id}/internal-api`, `PathPrefix /api/v1/svc/resource`, the public and private gateways, the suggestion `@ForbiddenRoute({PUBLIC, PRIVATE})`, and `autoGenerateAuthorizationPolicies`

#### Scenario: Exposure that no DENY rule can fix
- **WHEN** PUBLIC route `/api/files/{name}.txt` is declared, and `@ForbiddenRoute` forbids `/api/files` on all gateways, or `autoGenerateAuthorizationPolicies` is set
- **THEN** the build fails with an error that `/api/files/{name}.txt` can't be expressed, because variables must take up whole path segments

#### Scenario: Multiple problems
- **WHEN** a project has one conflict and two missing DENY rules
- **THEN** all three errors are logged, and the build fails with `3 route migration errors, see log`
