# Spec Delta

## Purpose

Makes sure the generated Istio configuration routes every request the same way the legacy Cloud-Core Service Mesh did on each gateway. The build fails with actionable diagnostics when it doesn't, so migrated services cannot silently expose endpoints or change routing.

## ADDED Requirements

### Requirement: Legacy routing model
The plugin SHALL build a legacy routing model for each border gateway from the scanned annotations. The model reproduces legacy route registration:

- A border route of type T (PUBLIC, PRIVATE, INTERNAL) is **allowed** on the gateways that T exposes: PUBLIC → public, private, internal; PRIVATE → private, internal; INTERNAL → internal.
- The same route is **forbidden** (legacy `allowed: false`, 404) on the other border gateways.
- When an allowed and a forbidden entry have the same gateway path on the same gateway, the allowed entry wins.
- Every `@ForbiddenRoute` declaration adds a forbidden entry on each gateway it lists.
- Facade and composite routes are not part of the model. Istio has no facade or composite gateway, and these routes become service-bound rules that are not compared with legacy routing.

Legacy matching SHALL treat a gateway path as a pattern:

- A path variable matches exactly one non-empty path segment.
- The pattern matches a request path equal to it, or any path that continues it with `/`.
- Among matching entries, the entry with the longest gateway path string wins, with variables counted in their `{name}` source form.
- When several matching entries with different gateway paths share the longest length, they SHALL decide the request only if they agree on allowed/forbidden and on the upstream path. The decision then uses the largest of their timeouts. Otherwise the legacy decision is undefined.

#### Scenario: Narrower type below a wider prefix
- **WHEN** PUBLIC route `/api/v1/svc/resource` and INTERNAL route `/api/v1/svc/resource/{id}/internal-api` are declared
- **THEN** in the legacy model, request `/api/v1/svc/resource/1/internal-api` is forbidden on the public and private gateways and routed on the internal gateway

#### Scenario: Longer allowed route below a forbidden one
- **WHEN** PUBLIC route `/api/v1/svc/resource`, INTERNAL route `/api/v1/svc/resource/{id}/internal-api` and PUBLIC route `/api/v1/svc/resource/{id}/internal-api/status` are declared
- **THEN** in the legacy model, request `/api/v1/svc/resource/1/internal-api/status` is routed on all three border gateways

### Requirement: Undefined legacy precedence is an error
When the legacy decision for a request path on a gateway is undefined, the plugin SHALL report a `LEGACY_PRECEDENCE_UNDEFINED` error and fail the build. The error SHALL name the gateway, an example request path, and the tied routes or forbidden declarations with their sources. The plugin SHALL NOT report an Exposure, Conflict or Lost route for request paths whose legacy decision is undefined.

#### Scenario: Tied routes with different upstream paths
- **WHEN** PUBLIC routes `/s/{a}/b` → `/one/{a}/b` and `/s/b/{a}` → `/two/{a}` are declared
- **THEN** the build fails with a `LEGACY_PRECEDENCE_UNDEFINED` error on the public, private and internal gateways for a request such as `/s/b/b`, naming both routes

#### Scenario: Tied allowed and forbidden entries
- **WHEN** PUBLIC route `/s/{a}/b` and INTERNAL route `/s/b/{a}` are declared
- **THEN** the build fails with a `LEGACY_PRECEDENCE_UNDEFINED` error on the public and private gateways for a request such as `/s/b/b`, and no such error on the internal gateway, where both routes are allowed with the same upstream path

#### Scenario: Tied routes that differ only in timeout
- **WHEN** PUBLIC routes `/s/{a}/b` → `/x/{a}/b` with timeout 5000 ms and `/s/b/{a}` → `/x/b/{a}` with timeout 60000 ms are declared
- **THEN** no `LEGACY_PRECEDENCE_UNDEFINED` error is reported, and in the legacy model request `/s/b/b` is routed to `/x/b/b` with timeout 60000 ms

### Requirement: Istio routing model
The plugin SHALL build an Istio routing model for each border gateway from the generated HTTPRoute rules attached to that gateway and the generated AuthorizationPolicy DENY rules that target it. A request SHALL be evaluated in this order:

1. If a DENY rule matches (the request matches `paths` and does not match `notPaths`), the request is denied.
2. Otherwise, a matching `Exact` rule wins.
3. Otherwise, the matching `PathPrefix` rule with the longest value wins. `PathPrefix` matches whole path segments only.
4. Otherwise, the request is unrouted.

#### Scenario: Exact wins over a longer prefix
- **WHEN** a gateway has rules `Exact` `/a` and `PathPrefix` `/a`
- **THEN** request `/a` is served by the `Exact` rule, and request `/a/1` is served by the `PathPrefix` rule

### Requirement: Differential validation over request paths
The plugin SHALL compare the legacy and Istio models on every border gateway for a set of request paths derived from all declared gateway paths, forbidden paths and generated match values. The set SHALL include at least:

- every such path, with its variables replaced by a sample segment;
- the path followed by `/`;
- the path followed by one extra segment;
- every intermediate prefix of the path at a segment boundary;
- every path in which a variable is replaced by a literal segment that another declared path has at the same position.

For each request path and gateway, the plugin SHALL classify the outcome:

- **Exposure** (error): legacy forbids the request or does not route it, and Istio routes it.
- **Conflict** (error): legacy and Istio both route the request, but the upstream path after rewriting differs. Both models route only to this service, so backends are never compared.
- **Lost route** (error): legacy routes the request, and Istio denies it or does not route it.
- **Timeout change** (warning): legacy and Istio both route the request to the same upstream path, with different timeouts.
- **Equivalent**: every other combination. This includes legacy forbidden vs Istio denied or unrouted.

Routes of other services on the same gateway, and legacy routes that lead to another service or use header matchers, SHALL NOT be modeled. The plugin can't generate them, so validation does not detect overlaps with them.

#### Scenario: Cut exposes controller subtree
- **WHEN** only PUBLIC route `/api/v1/svc/order/{id}/items` is declared under `/api/v1/svc/order`, and no `@ForbiddenRoute` covers it
- **THEN** validation reports an Exposure error on the public, private and internal gateways for request paths such as `/api/v1/svc/order` and `/api/v1/svc/order/<sample>`, and the build fails

#### Scenario: Cut exposure already covered by a shorter legacy route
- **WHEN** PUBLIC route `/api/v1/svc/order` and PUBLIC route `/api/v1/svc/order/{id}/items` are declared with the same rewrite
- **THEN** validation reports no Exposure error for `/api/v1/svc/order/<sample>`

#### Scenario: Implicit legacy 404 not declared
- **WHEN** PUBLIC route `/api/v1/svc/resource` and INTERNAL route `/api/v1/svc/resource/{id}/internal-api` are declared, and no `@ForbiddenRoute` covers the internal one
- **THEN** validation reports an Exposure error on the public and private gateways for `/api/v1/svc/resource/<sample>/internal-api`, and the build fails

#### Scenario: Implicit legacy 404 for a path without variables
- **WHEN** PUBLIC route `/api/v1/svc/resource` and INTERNAL route `/api/v1/svc/resource/admin` are declared, and no `@ForbiddenRoute` covers the internal one
- **THEN** validation reports an Exposure error on the public and private gateways for `/api/v1/svc/resource/admin`

#### Scenario: Narrower route with no wider prefix above it
- **WHEN** only INTERNAL route `/api/v1/svc/admin/{id}` is declared, and nothing else covers `/api/v1/svc/admin`
- **THEN** no Exposure error is reported on the public or private gateway, but an Exposure error is reported on the internal gateway for `/api/v1/svc/admin`, which the cut exposes

#### Scenario: Conflicting behavior after the cut
- **WHEN** PUBLIC route `/api/v1/svc/resource` → `/resource` and PUBLIC route `/api/v1/svc/resource/{id}/v2-api` → `/v2/{id}/v2-api` of the same service are declared, which cannot share a single prefix rewrite
- **THEN** validation reports a Conflict error on all three border gateways for `/api/v1/svc/resource/<sample>/v2-api`, and the build fails

#### Scenario: Literal and variable at the same position
- **WHEN** route B `/a/b/{id}/c` → `/x/{id}/c` and route C `/a/b/lit` → `/y` are declared on the same gateway
- **THEN** request `/a/b/lit/c` is evaluated: legacy routes it by B (the longer pattern), Istio routes it by C (the longer prefix), and a Conflict error is reported

#### Scenario: Only timeouts differ
- **WHEN** two routes collapse into one merged rule with the largest timeout
- **THEN** validation reports Timeout change warnings for the affected request paths, and the build does not fail because of them

### Requirement: Contradictory declarations are errors
The plugin SHALL report an error when a `@ForbiddenRoute` declaration forbids, on some gateway, exactly the gateway path of a route allowed on that gateway. The plugin SHALL also report an error when two legacy entries of equal gateway path length both match the same request path on a gateway with different results, because legacy precedence between them is undefined.

#### Scenario: Forbidding an allowed route's own path
- **WHEN** a method has `@Route(RouteType.PUBLIC)` and `@ForbiddenRoute(RouteType.PUBLIC)` with the same gateway path
- **THEN** the build fails with an error that names the method, the path and the public gateway

### Requirement: Legacy-invalid duplicate routes are errors
The plugin SHALL report a `LEGACY_INVALID` error when two routes are both allowed on the same gateway with the same gateway path and different service paths, because the legacy runtime rejects this configuration too. The plugin SHALL report one error per gateway and gateway path, naming both routes and their sources. The plugin SHALL NOT report Conflict or `LEGACY_PRECEDENCE_UNDEFINED` errors that involve that gateway path, on any gateway, so the problem isn't reported twice; after it is fixed, the next build reports what remains. Routes with the same gateway path and service path that differ only in timeout SHALL NOT be reported as legacy-invalid.

#### Scenario: Same gateway path, different service paths
- **WHEN** INTERNAL routes `/api/x` → `/a` and `/api/x` → `/b` are declared
- **THEN** the build fails with one `LEGACY_INVALID` error for `/api/x` on the internal gateway, and no Conflict error for `/api/x`

#### Scenario: Undefined precedence involving a legacy-invalid gateway path
- **WHEN** INTERNAL routes `/s/{a}/b` → `/one/{a}/b`, `/s/{a}/b` → `/other/{a}/b`, `/s/b/{a}` → `/two/{a}` and `/s/b` → `/two` are declared
- **THEN** the build fails with a `LEGACY_INVALID` error for `/s/{a}/b` and no other error

#### Scenario: Only one of them allowed on a gateway
- **WHEN** PUBLIC route `/api/x` → `/a` and INTERNAL route `/api/x` → `/b` are declared
- **THEN** `LEGACY_INVALID` is reported for the internal gateway, where both are allowed, and not for the public or private gateway

### Requirement: Legacy-invalid annotations are errors
The plugin SHALL report a `LEGACY_INVALID` error naming the class or method when a route annotation has a non-empty `hosts` attribute together with a border gateway name in `gateways`. The legacy runtime rejects this too ("Only composite gateway can have hosts"). This error SHALL be reported together with all other findings, not instead of them.

#### Scenario: Hosts on a border gateway
- **WHEN** a method has `@Route(gateways = "public-gateway-service", hosts = "example.com")`
- **THEN** the build fails with a `LEGACY_INVALID` error naming the method

#### Scenario: Hosts on a composite gateway
- **WHEN** a method has `@Route(gateways = "composite-gw", hosts = "example.com")`
- **THEN** no `LEGACY_INVALID` error is reported for it

### Requirement: Actionable failure report
When validation finds errors, the plugin SHALL log all findings at once, grouped by gateway, and then fail the build. Each finding SHALL include:

- the finding kind;
- the gateway;
- one example request path;
- the winning legacy entry: gateway path, route type, allowed or forbidden, rewrite, and source (fully qualified class name and method name when applicable);
- the winning Istio rule or DENY rule: match type and value, rewrite, and source routes;
- a suggested remediation.

For an Exposure, the remediation SHALL name the class or method to annotate and the exact `@ForbiddenRoute(...)` gateways to add. While `autoGenerateAuthorizationPolicies` is `false`, it SHALL also offer enabling that parameter. When the exposed legacy forbidden route has a gateway path that neither `@ForbiddenRoute` nor an automatic DENY rule can forbid (a partial-segment variable), the remediation SHALL instead ask to change that gateway path, name the route and its source, and say why no DENY rule can be used. For a Lost route caused by an automatic DENY rule, the remediation SHALL say the rule was generated automatically and ask to change the gateway paths involved; it SHALL NOT ask to narrow a `@ForbiddenRoute` declaration. For a Conflict, the remediation SHALL name the colliding routes. It SHALL suggest aligning their gateway paths or rewrites, or migrating the affected waypoint manually to `VirtualService` outside this plugin. Findings SHALL be deduplicated so that the same (kind, gateway, legacy entry, Istio rule) combination appears only once.

#### Scenario: Exposure report content
- **WHEN** the implicit 404 scenario above fails validation
- **THEN** the log names the INTERNAL method, the public and private gateways, request path `/api/v1/svc/resource/<sample>/internal-api`, the Istio rule `PathPrefix /api/v1/svc/resource` that exposes it, the suggestion `@ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE})` on that method, and the alternative `<autoGenerateAuthorizationPolicies>true</autoGenerateAuthorizationPolicies>`

#### Scenario: Exposure that no DENY rule can fix
- **WHEN** PUBLIC route `/api/files` and PRIVATE route `/api/files/{name}.txt` are declared, with or without `autoGenerateAuthorizationPolicies`
- **THEN** the Exposure error on the public gateway has one remediation: change the gateway path `/api/files/{name}.txt` of the PRIVATE route, because forbidden paths need whole-segment variables

#### Scenario: Multiple problems
- **WHEN** a project has one Conflict and two Exposure findings
- **THEN** all three findings are logged before the build fails
