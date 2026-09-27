# Design

## Context

For motivation, see `proposal.md` (Why) and `core-maven-plugins/httproutes-generator-maven-plugin/regex-routes-migration.md` (Option 1 was chosen). Unless stated otherwise, plugin paths and classes below are in `core-maven-plugins/httproutes-generator-maven-plugin`. Current state of the plugin:

- `RouteScanner` builds a flat `Set<HttpRoute>` (`path`, `gatewayPath`, `type`, `timeout`) from `@Route`, Spring/JAX-RS mappings, `@Gateway` and `@GatewayRequestMapping`. It does not keep where each route came from. It skips classes that have no `@Route`. It never reads the `@Routes` container, `@Route` `gateways`/`hosts`, `@FacadeRoute`, `@FacadeGateway` or `@FacadeGatewayRequestMapping`. For `@Route(RouteType.FACADE)` it takes the gateway path from `@Gateway`/`@GatewayRequestMapping`.
- `HttpRouteRenderer` groups routes by `Type` into one HTTPRoute per type. The parentRefs follow the exposure chain: PUBLIC → public-gateway, private-gateway and Service internal-gateway-service; PRIVATE → private-gateway and internal-gateway-service; INTERNAL → internal-gateway-service; FACADE → Service `{{ .Values.SERVICE_NAME }}`. It turns `{var}` into `([^/]+)` inside a `RegularExpression` match. It always uses `ReplacePrefixMatch` with the full service path, which is invalid for regex. FACADE routes whose `path == gatewayPath` are skipped, even when the FACADE HTTPRoute exists, so Istio returns 404 for them.
- Legacy facade and composite routes (`ClassRoutesSet`): both have `RouteType.FACADE`. A facade route (`@Route(RouteType.FACADE)` or `@FacadeRoute` without `gateways`) goes to the facade gateway named after the microservice, and its gateway path comes from `@FacadeGateway`/`@FacadeGatewayRequestMapping`, or equals the service path when there is none. A composite route (`gateways` with any name other than a border gateway name) goes to that gateway, with the gateway path from `@Gateway`/`@GatewayRequestMapping` and optional `hosts`. A border name in `gateways` gives a border route of that type (`RouteType.fromGatewayName`). A method-level `@Route` list replaces the class-level one.
- Legacy registration (`route-registration-common` `RouteTransformer`) registers every border route on all three border gateways, with `allowed=false` on gateways wider than the route type. It prefers `allowed=true` when a gateway path is registered twice. Annotations carry no header matchers. `HttpRouteRenderer` puts the same single `backendRefs` entry (`backendRefVal`:`servicePort`) on every rule. So route "behavior" is only forbidden vs allowed, the upstream path (rewrite) and the timeout. The migration document's "different cluster" and "header matcher" overlaps (routes 3 and 4 of its example, both to `another-service`) can't come from this plugin.
- The plugin depends on `route-registration-common` `7.5.2-SNAPSHOT` from the same monorepo (`core-rest-libraries/route-registration/route-registration-common`).

## Goals / Non-Goals

**Goals:**
- One pipeline where generation and validation share the same route model, so that what we validate is exactly what we render.
- Validation that catches ordering interactions (cut, merge, Exact, DENY/notPaths) without a separate hand-written rule for each case in the migration document.
- Diagnostics precise enough that a developer can fix the build without reading the migration document.

**Non-Goals:**
- Reproducing the Envoy string-prefix behavior of legacy prefixes without variables (legacy `/api/users` also matched `/api/usersX`). Both models match whole segments, and this difference is not reported.
- Routes of other services on the same gateway, and the gateway's own `/` fallback route. "Unrouted" means "not routed to this service".
- Fixing existing scanner deviations from legacy path resolution. For example, `buildClassGatewayRoutes` uses only the first class mapping. The legacy model is built from what the scanner produces. The annotation forms the scanner didn't read at all are read now (D13), and facade gateway paths follow legacy.
- Modeling facade or composite gateways, or comparing facade and composite routes with legacy routing. Istio has none of these gateways; the routes become service-bound rules (D15).
- A catch-all rule on the service-bound HTTPRoute (user decision). Paths that no facade or composite rule matches return 404 while that HTTPRoute exists.
- Generating a `VirtualService` fallback. Conflicts are reported, and any fallback is manual.
- Overlaps with routes that lead to another service (different legacy `cluster`) or that differ only by header matchers. The plugin can't produce them (see Context), so no model, planner step, finding kind or test covers them. There is no backend field in `HttpRoute`, the planned rules or the decisions, and no header matchers are modeled.
- A switch to turn validation errors into warnings. The opt-in `autoGenerateAuthorizationPolicies` (D12) removes the largest class of failures, exposures, by fixing them instead of hiding them.

## Decisions

### D1. Pipeline: scan → model → plan → validate → render
`GenerateRoutesMojo.execute()` becomes:

1. `RouteScanner.collectDeclarations(reactorProjects)` returns a `RouteDeclarations` DTO (D14) with:
   - `List<DeclaredRoute>`: an unchanged `HttpRoute` plus the sorted set of origins (`class#method` or `class`) that declared it;
   - `List<ForbiddenDeclaration>`: gateway path, set of target gateways, origin;
   - scan-time findings (legacy-invalid `hosts`, D13; invalid `@ForbiddenRoute` usage, spec: Invalid ForbiddenRoute usage).
2. `LegacyRouteTable.build(declarations)`: per `Gateway` (a new enum PUBLIC, PRIVATE, INTERNAL with Istio ref data), a list of `LegacyEntry(pattern, allowed, route|forbidden, origin)`. FACADE routes are not added to any table.
3. `IstioRoutePlanner.plan(declarations, legacyTables)` produces an `IstioPlan`:
   - HTTPRoute rules per route type, each rule with `MatchType`, value, rewrite kind and value, timeout, and source routes. FACADE rules are planned for the service-bound target (D15);
   - `DenyRule`s per gateway, from `@ForbiddenRoute` (D8) and, when `autoGenerateAuthorizationPolicies` is `true`, automatic ones (D12);
   - generation-time findings (unresolved same-match conflicts, legacy-invalid duplicates).
4. `MigrationValidator.validate(legacyTables, plan)` returns a list of `Finding`s.
5. If there is any finding with severity ERROR: log the report and throw `MojoFailureException`. Nothing is written.
6. Otherwise, `HttpRouteRenderer` renders the planned rules (it no longer takes raw `HttpRoute`s), and the new `AuthorizationPolicyRenderer` renders the deny rules. Both are written into one file in the existing header and guard.

*Alternative*: keep the rendering-from-`Set<HttpRoute>` API and add checks around it. Rejected: then validation would re-derive the rendered rules and could drift from them.

`MojoFailureException` (not `MojoExecutionException`) is used because the failure is caused by the project's configuration, not by the plugin crashing. `execute()` declares both exceptions. Scan, plan and validation run before `writeRoutesFile`, so its `catch (Exception e)` → `MojoExecutionException` wrapper can't turn a validation failure into an execution error. That wrapper stays only around rendering and file I/O.

### D2. Validate by differential simulation, not pairwise overlap rules
Both models expose `Decision decide(Gateway, String requestPath)`. A decision is one of:
- `Routed(upstreamPath, timeout, source)`. There is no backend: every routed decision goes to this service;
- `Forbidden(source)` (legacy) or `Denied(rule)` (Istio);
- `Unrouted`.

The validator runs both models on a generated set of probe paths and classifies each pair as the spec describes. The limitations in the migration document that the plugin can produce (cut conflicts between rewrites of this service, forbidden blocking longer routes, cut exposure, implicit 404s) all show up as a probe whose legacy and Istio decisions differ, so no special cases are needed. The header-matched root takeover (limitation 2 in the document) needs a header matcher and another service, so it can't happen here. Only its same-service form (different rewrite or timeout on the root) remains, and D7 handles it.

*Alternative*: implement each limitation in the document as its own check. Rejected: those checks are ad hoc, easy to get incomplete (for example the literal-vs-variable case `/a/b/{id}/c` vs `/a/b/lit`), and they don't verify the generated DENY `notPaths` or `Exact` rules.

### D3. Probe generation
Collect all patterns: legacy gateway paths, forbidden paths and generated match values. For each pattern, split it into segments. For each prefix `p` of the segment list (including the full pattern), emit these probes:
- `p`
- `p + "/"`
- `p + "/" + SAMPLE`

Here `SAMPLE` is a literal that collides with no declared literal segment (for example `x-probe-0`, with the number increased until it is unique).

Each variable segment in `p` is instantiated as:
1. `SAMPLE`;
2. every literal segment that another pattern has at the same depth under the same literal prefix. This finds literal-vs-variable precedence flips.

For partial-segment variables (`{name}.txt`), only the variable part of the segment is substituted. The cartesian product over variables is capped by instantiating one variable at a time with literals while the other variables keep `SAMPLE`. This keeps the probe count linear in patterns × segments × colliding literals. Probes are deduplicated in a `TreeSet`, so findings come out in a deterministic order.

*Why this is sufficient*: decisions in both models change only at pattern boundaries (segment-aligned prefixes of patterns), and a variable decides differently only between "some literal another pattern names" and "anything else". The probe set has one representative for each such class.

### D4. Legacy decision semantics
- Pattern → segment matcher: a variable matches `[^/]+` (partial segments use the legacy regex form). A match needs the request to equal the pattern or to continue it with `/`.
- Winner: the longest `pattern.length()` (source text, including `{name}`).
- An allowed entry and a forbidden entry with the same pattern collapse to the allowed one (legacy `RouteTransformer`). The exception is an explicit `@ForbiddenRoute` on an allowed route's exact pattern, which is a generation-time error (spec: Contradictory declarations).
- Ties in length between different patterns that match the same request: legacy picks one in an undefined order. If the tied entries agree on allowed/forbidden and on the upstream path, the decision is that one, with the largest of their timeouts (no finding). Otherwise the decision is *undefined*, and the validator reports one `LEGACY_PRECEDENCE_UNDEFINED` ERROR per (gateway, tied patterns), naming the entries and their origins, with one example request. It reports no other finding for probes whose legacy decision is undefined, so the same problem isn't also reported as an Exposure, Conflict or Lost route.
- Two **allowed** entries on the same gateway with the same pattern string and different service paths are invalid in legacy too: `RouteTransformer.validateAndReturnRoutes` throws "several target paths for forwarding from the same source path". The planner reports one `LEGACY_INVALID` ERROR per (gateway, pattern), naming both routes and their origins. Conflict and `LEGACY_PRECEDENCE_UNDEFINED` findings carry the gateway paths they involve. `RouteMigration` drops, in one filter step after planning and validation, every such finding that involves a legacy-invalid gateway path (on any gateway), so the same problem isn't reported twice or blamed on the migration. Once the legacy-invalid path is fixed, the next build reports whatever problems remain for it. Same pattern and same service path with different timeouts is valid in legacy (the first registered route wins, in an undefined order), so it goes through the D7 max-timeout merge.
- Upstream path: reproduce the legacy regex rewrite. Captured variable values replace the variables of the service path by position, and the rest of the request path is appended unchanged.

### D5. Istio decision semantics
DENY check first. Path templates are evaluated as Istio does: `{*}` is one segment, `{**}` is zero or more segments and appears only at the end, and `notPaths` excludes. Then `Exact`, then the longest segment-aligned `PathPrefix`.

Upstream path:
- `ReplacePrefixMatch`: replace the matched prefix, and normalize a double `/` the way Istio does when the replacement is `/`.
- `ReplaceFullPath`: the static value.
- No filter: unchanged.

### D6. Cut and rewrite derivation
`cut(path)` = the part of `path` before the segment containing the first `{`, stripped of its trailing slash, or `/` if that is empty. A path without variables only loses its trailing slash (`/api/items/` → `/api/items`), because legacy matches `/x/` and `/x` the same way and Gateway API ignores a trailing slash in `PathPrefix`. Without this, `@GetMapping("/")` and `@GetMapping("/{id}")` in a controller mapped to `/api/items` would give two `PathPrefix` rules that match the same requests, and Istio would pick one in an undefined order. Istio path templates in DENY rules (D8) drop the trailing slash too. `match = cut(gatewayPath)`. If the gateway path and service path differ, `rewrite = cut(servicePath)`.

Example: `/api/v1/{id}/items` → `/items/{id}/items` gives match `/api/v1` and rewrite `/items`.

This is correct only when the part after the cut is the same in both paths (variables compared by position), because `ReplacePrefixMatch` keeps everything after the match unchanged. That holds for the routes services declare in practice, so the plugin doesn't check it, to keep the code simple.

*Alternative*: compare the remainders and fail the build when they differ. Rejected per user decision: the case doesn't occur in practice.

### D7. Grouping, merging and the Exact split
Routes are grouped by their cut value `P` across all border route types (the service-bound HTTPRoute of D14 groups its routes separately). Grouping across types instead of per gateway is simpler and gives the same result: exposure sets are nested chains, so every gateway that has two routes of a group has them in the same group, and the merged rule goes into the widest type's resource (see below). For every `P` that more than one route produces:

1. If all routes have the same rewrite and timeout, emit one rule.
2. Else, if exactly one route `S` has `gatewayPath == P` or `gatewayPath == P/` (Spring `@GetMapping("/")`), and at least one other route has `gatewayPath == P/{var}`, and all routes other than `S` share one rewrite: apply the Exact split to `S`. It becomes two `Exact` rules with `ReplaceFullPath`, keeping S's timeout. The rest are merged into the `PathPrefix` rule, and if their timeouts differ, the max is used with a warning.
3. Else, if all routes have the same rewrite: merge them with the max timeout and log a WARNING.
4. Otherwise, report a CONFLICT finding and emit nothing for `P`. The build fails anyway.

The Exact split is the same-service form of the document's limitation 2. It keys only on rewrite and timeout, never on backend or headers. It is safe on every gateway where S appears: in legacy, the `P/{var}` entry exists on every border gateway (allowed or forbidden), and because it is longer it takes every request below `P/`. So S only ever received `P` and `P/`.

A merged rule goes into the widest type's HTTPRoute resource. This works because exposure sets are nested chains (INTERNAL ⊂ PRIVATE ⊂ PUBLIC), so the union of gateways equals the widest type's parentRefs. This removes identical matches spread across several HTTPRoute resources, where Istio would pick one by creation time. On the wider gateways, the narrower routes of the group are then routed where legacy forbade them; validation reports that as Exposure unless DENY rules (D8, D12) cover it.

*Alternative for timeout-only differences*: always fail. Rejected per user decision (merge with the max, warn).

The Exact split is kept for same-service routes (user decision). It is the only producer of `Exact` matches and `ReplaceFullPath` filters.

### D8. DENY rule construction
For each `ForbiddenDeclaration` × gateway:
- `paths = [F', F' + "/{**}"]`, where `F'` replaces each `{...}` with `{*}` (`/` → `["/", "/{**}"]`).
- `notPaths` = for every legacy allowed entry Q on that gateway with `len(Q) > len(F)` and `overlaps(F, Q)`: add `[Q', Q' + "/{**}"]`.
- `overlaps(F, Q)` compares the segments up to the shorter of the two patterns: two variables always overlap, a variable and a literal overlap if the variable's segment pattern matches the literal, and two literals must be equal. It doesn't instantiate Q with a sample, because a sample misses a literal of F in the same segment as a variable of Q: forbidden `/a/lit/x` with allowed `/a/{id}/x/y` needs `/a/{*}/x/y` in `notPaths`, or the DENY rule takes `/a/lit/x/y`, which legacy routes. Where Q' is wider than what legacy routed through Q under F (Q' also excludes `/a/other/x/y`), the excluded requests are outside F's `paths` anyway, so nothing extra is let through.

There is one rule per forbidden path, not one rule with all paths merged. `notPaths` belongs to a specific forbidden path, and Istio ORs rules within a policy. Nested forbidden paths therefore work without extra handling.

### D9. `@ForbiddenRoute` lives in route-registration-common
Add `ForbiddenRoute.java` next to `Route.java`: `@Retention(RUNTIME)`, `@Target({TYPE, METHOD})`, `@Documented`, `RouteType[] value()` with no default. Retention must be RUNTIME because `RouteScanner` uses `disableRuntimeInvisibleAnnotations()`. Legacy processing classes (`ClassRoutesBuilder` and others) are not changed, so the legacy runtime ignores it. The plugin references it by `ForbiddenRoute.class.getName()`, like `Route`.

The scanner's `hasRoute` filter becomes `hasRoute || hasForbiddenRoute` (class or method level). Forbidden gateway paths are resolved with the same helpers as `buildClassLevelRoutes` and `buildRoutesForMethod`, extracted into shared path-resolution methods.

*Alternative*: a separate annotations artifact owned by the plugin. Rejected per user decision: services already depend on route-registration-common.

### D10. AuthorizationPolicy rendering
The new renderer uses Jackson records, like `HTTPRouteResource`. `security.istio.io/v1`, `kind: AuthorizationPolicy`, `spec.targetRefs`, `spec.action: DENY`, and `spec.rules[].to[].operation.{ports, paths, notPaths}`. Policies are sorted public, private, internal. Labels come from `buildRouteLabels`, moved to a shared helper.

New Mojo parameters, all set in the plugin `<configuration>` in `pom.xml`, with no user property, like the existing ones:
- `authorizationPolicyPorts`: `Map<String, String>`, optional, no default value in the annotation. Keys are border gateway names (`public-gateway-service`, `private-gateway-service`, `internal-gateway-service`, the names used in `@Route(gateways = ...)`). Values are comma-separated port lists; whitespace around ports is trimmed. A gateway that isn't listed, or the whole parameter when it isn't configured, uses `8080`. An unknown key, an empty value or a port outside 1–65535 is a configuration ERROR that fails the build before scanning. The DENY rules of each gateway's policy use that gateway's ports. For example:

  ```xml
  <authorizationPolicyPorts>
    <public-gateway-service>8080</public-gateway-service>
    <internal-gateway-service>8080,8443</internal-gateway-service>
  </authorizationPolicyPorts>
  ```

  Here private-gateway-service uses `8080`. Maven fills a `Map<String, String>` from child element names, so no custom converter is needed.

  *Alternative*: one list for all gateways. Rejected per user decision: the Gateway listener port and the internal-gateway-service port can differ.
- `autoGenerateAuthorizationPolicies`: `boolean`, `@Parameter(defaultValue = "false")`. See D12.

### D11. Diagnostics format
Findings are grouped by gateway, then kind, then sorted. They are logged at ERROR level (WARN for timeout warnings) as a block, for example:

```
[ROUTE-MIGRATION] EXPOSURE on public-gateway
  request:  /api/v1/svc/resource/x-probe-0/internal-api (any method)
  legacy:   FORBIDDEN (implicit: INTERNAL route /api/v1/svc/resource/{id}/internal-api from com.acme.ResourceController#internalApi)
  istio:    ROUTED by PathPrefix /api/v1/svc/resource -> /resource (from com.acme.ResourceController)
  fix:      add @ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE}) to com.acme.ResourceController#internalApi
```

For exposures caused by the cut, where legacy has no entry, the fix suggests `@ForbiddenRoute(<gateways>)` on the class whose mapping equals the cut prefix. If no such class exists, it suggests a method or class mapped to that prefix. While `autoGenerateAuthorizationPolicies` is `false`, every Exposure fix that suggests a `@ForbiddenRoute` also offers the alternative `or set <autoGenerateAuthorizationPolicies>true</autoGenerateAuthorizationPolicies>`.

Some exposures can't be fixed with a DENY rule: an implicit forbidden route with a partial-segment variable (`/api/files/{name}.txt`) can't be written as an Istio path template, `@ForbiddenRoute` rejects it (spec: forbidden paths need whole-segment variables) and D12 generates no rule for it. For those, the fix asks to change the gateway path of the named route and says why neither option works. It doesn't offer `autoGenerateAuthorizationPolicies`, which wouldn't help.

A Lost route caused by an automatic DENY rule (D12) says the rule was generated automatically and asks to change the gateway path of the route or of the routes the rule comes from. It doesn't ask to narrow a `@ForbiddenRoute` declaration, because the user never wrote one.

The exception message summarizes the counts and points to the log.

### D12. Opt-in automatic DENY rules (`autoGenerateAuthorizationPolicies`)
When the parameter is `true`, `IstioRoutePlanner` adds automatic `DenyRule`s after the HTTPRoute rules are planned and before validation. It evaluates the Istio model *without* any DENY rules (routing only), so the result doesn't depend on the order rules are added. Automatic rules are only for border gateways:

1. **Implicit forbidden routes.** For every legacy forbidden entry F that comes from a route type (not from `@ForbiddenRoute`) on gateway G: if the routing-only Istio model routes F instantiated with `SAMPLE`, or F + `/` + `SAMPLE`, add a rule for F. It is built exactly as D8 builds it, including `notPaths` from the longer allowed entries below F. Implicit entries that Istio doesn't route anyway (for example an INTERNAL route with no wider prefix above it) get no rule, which keeps policies small. Entries with a partial-segment variable get no rule either: `{*}` stands for a whole segment, so a rule for `/api/files/{name}.txt` would deny every file below `/api/files`, including the ones legacy routes. The validator reports them as Exposure with the fix of D11.
2. **Cut exposure.** For every planned `PathPrefix` value P on gateway G that has at least one source route whose gateway path contains a variable: if the legacy model does not route P on G, add a rule with `paths = [P, P + "/{**}"]`. Its `notPaths` are `[Q', Q' + "/{**}"]` for every legacy allowed entry Q on G that lies below P and is longer than P. This is limitation 4 of the migration document: when a shorter allowed route covers P, legacy already exposed the subtree, and no rule is added. Checking P alone is enough: a shorter route that routes paths below P also routes P, and longer allowed routes below P stay reachable through `notPaths`. An allowed route below P that isn't longer than P (for example a catch-all `/{a}/{b}/{c}/{d}` from another class) isn't in `notPaths`. If legacy routes a request below P through it, the rule denies that request and the validator reports a Lost route (D11).

Rules are deduplicated by (gateway, paths, notPaths). An explicit `@ForbiddenRoute` rule wins over an identical automatic one, so it keeps its origin. Automatic rules carry a synthetic origin (`auto: implicit forbidden of <route origin>` or `auto: cut exposure of <P> from <route origins>`) for diagnostics. The contradictory-declaration check (D4) applies only to explicit declarations. Validation runs on the combined plan unchanged, so it verifies automatic rules like any other.

*Default `false`*: generated DENY rules on shared border gateways are a security-relevant change that service owners should opt into explicitly. By default, the build keeps reporting these cases, with both fixes offered.

*Alternative*: always generate automatic rules and drop `@ForbiddenRoute`. Rejected per user decision: explicit annotations stay the default, and automation is opt-in.

### D13. Reading all legacy route annotation forms
`hasRoute` selects classes with `Route`, `Routes` (the `@Repeatable` container, present whenever an element has two or more `@Route`) or `FacadeRoute`, at class or method level, plus `ForbiddenRoute` (D9). Per element, the scanner reads a list of route annotations: every `@Route`, from `Route` or `Routes`, and `@FacadeRoute`, which is a `@Route(RouteType.FACADE)` with its own `gateways`. A method-level list replaces the class-level list, as in legacy. Each entry is resolved with the same rules a single `@Route` has today (type, timeout and the class fallback), reading `getParameterValues(false)`. A missing `gateways`/`hosts` attribute, or an array that holds only empty strings, counts as empty.

Each entry becomes one declaration per resolved target:
- `gateways` empty, type PUBLIC/PRIVATE/INTERNAL: a border route of that type, with the gateway path from `@Gateway`/`@GatewayRequestMapping` (unchanged).
- `gateways` empty, type FACADE: a **facade** route.
- Every name in `gateways`: a border name (`public-gateway-service`, `private-gateway-service`, `internal-gateway-service`) gives a border route of `RouteType.fromGatewayName`, with `@Gateway` paths. Any other name gives a **composite** route.

A facade route takes its gateway path from `@FacadeGateway`/`@FacadeGatewayRequestMapping`, resolved with the same helpers as `@Gateway`/`@GatewayRequestMapping`, or uses the service path when there is none. A composite route takes it from `@Gateway`/`@GatewayRequestMapping`. Both become `HttpRoute`s with `Type.FACADE`, so `HttpRoute` doesn't change. This fixes today's deviation, where FACADE used `@Gateway` paths.

Scan-time finding, reported in the normal report (D11) instead of throwing, so all problems show up in one build:
- `LEGACY_INVALID` (ERROR): non-empty `hosts` together with a border name in `gateways`. Legacy `AbstractRoutesBuilder.validateRouteHosts` throws "Only composite gateway can have hosts" for it.

Otherwise `hosts` are ignored: Istio has no composite gateway and no virtual hosts on a Service-bound HTTPRoute.

*Alternative*: keep failing the build on these forms. Rejected per user decision: facade and composite routes must produce service-bound rules (D15), and composite routes normally come as a second `@Route` next to a border one, so `@Routes` and `gateways` have to be read anyway.

### D14. Route origins without changing `HttpRoute`
`HttpRoute` stays a four-field record, with the same `equals`/`hashCode`. Internally the scanner collects `Map<HttpRoute, SortedSet<String>>`, so a route declared by several elements (for example through the superclass recursion in `getRequestMappingPaths`) collapses into one entry, as in today's `Set`, and keeps all its origins. `collectDeclarations` exposes it as `List<DeclaredRoute>`, sorted by the path-specificity comparator. `collectRoutes` stays, returns `Set<HttpRoute>` built from the same map, and is what `GenerateRoutesMojoTest` keeps using. The only assertion that changes is the FACADE route of `SpringTestController8#method1`: it has `@GatewayRequestMapping` but no facade path annotation, so under D13 its gateway path equals its service path, as in legacy.

*Alternative*: add `origin` to the record. Rejected: it changes equality, breaks `routes.contains(new HttpRoute(...))` and `routes.size()` assertions, and turns the same route found through two elements into two routes.

### D15. Service-bound HTTPRoute for facade and composite routes
All `Type.FACADE` declarations (facade and composite, D13) form one more planning target, `SERVICE`, next to the three border gateways. The planner applies D6 (cut and rewrite) and D7 (merge, Exact split, conflicts) to it exactly as to a gateway. The Exact split argument holds because all these routes are on the same target. The results are:
- If no planned `SERVICE` rule has a rewrite filter, no service-bound HTTPRoute is generated. Istio then sends every request for the Service to it unchanged, which is what these routes do anyway. If any of them has a timeout, one WARNING says that the timeouts are not applied.
- Otherwise, one HTTPRoute `{{ .Values.SERVICE_NAME }}-java-annotations-facade` with parentRef Service `{{ .Values.SERVICE_NAME }}` (as today) contains **every** planned `SERVICE` rule, including those without a rewrite. Once an HTTPRoute is bound to a Service, Istio returns 404 for requests that no rule matches, so dropping identity rules (today's behavior) breaks them.
- No catch-all `PathPrefix /` rule is added (user decision).

Legacy facade and composite gateways were separate gateways, so two routes with the same gateway path and different service paths were valid there, for example two composite gateways, or two hosts. On `SERVICE` they collide, and D7 step 4 reports a CONFLICT that names both routes and says they came from different legacy gateways. The same gateway path with the same service path and different timeouts goes through the max-timeout merge.

`SERVICE` is not part of the legacy model or the differential validation (D2–D5), and D8/D12 never generate DENY rules for it. Its entry point differs from legacy (clients call the Service, not a facade or composite gateway), so there is no legacy decision to compare with.

*Alternative*: add a catch-all rule so that other paths keep working through the Service. Rejected per user decision.

## Risks / Trade-offs

- [Many existing services start failing the build (implicit 404s, cut exposure, legacy-invalid annotations)] → This is intentional (they are insecure or silently wrong today). Every exposure failure names the exact `@ForbiddenRoute` to add or offers `autoGenerateAuthorizationPolicies`. The plugin is released as a new major version, and release notes and the README include a migration section.
- [A DENY rule on a shared border gateway also blocks another service's routes under the same path] → Out of scope per the single-service decision: gateway paths are service-namespaced (`/api/<version>/<service>/...`), and the plugin doesn't model other services. The README states it.
- [Automatic rules deny more than legacy did] → They are validated like explicit ones (Lost route findings), and the parameter defaults to `false`.
- [The probe set misses a class of paths] → The argument in D3 plus unit tests for every example in the migration document that the plugin can produce (all routes except the two `another-service` routes 3 and 4), plus the spec scenarios. The probe generator is isolated, so it can be extended without touching the models.
- [The legacy model differs from real legacy registration where the scanner already deviates] → Path-resolution deviations are out of scope (Non-Goals), and findings show the pattern and origin, so a mismatch is visible. All legacy annotation forms are read (D13).
- [While the service-bound HTTPRoute exists, requests to the Service on other paths return 404 (no catch-all, user decision)] → The README states it, and it names the fix: declare those endpoints as facade routes, or call them through a border gateway. Before this change, the same HTTPRoute already existed whenever a facade route had a rewrite, and it also dropped identity facade routes.
- [Facade routes that used `@Gateway` paths change their gateway path] → This is the legacy behavior, and the README migration section names it.
- [403 instead of 404 for forbidden paths] → Documented in the README as a known BWC difference.
- [DENY on the wrong listener port does not deny] → `authorizationPolicyPorts` is configurable per gateway, and the README explains that each entry must match that Gateway's listener port, or the internal-gateway-service port.
- [Two-module change (plugin + route-registration-common)] → Implement the annotation first. The plugin pom already tracks the monorepo SNAPSHOT, so both are released in the same train.

## Migration Plan

1. Add `@ForbiddenRoute` to route-registration-common and build it (SNAPSHOT).
2. Release the plugin as a new major version. Validation is always on. Consumers that upgrade get it.
3. Consumers fix build failures by adding `@ForbiddenRoute`, setting `autoGenerateAuthorizationPolicies` to `true`, or aligning paths.

## Open Questions

None.
