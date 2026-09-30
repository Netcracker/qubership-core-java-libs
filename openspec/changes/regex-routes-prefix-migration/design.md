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
- Generated config that keeps the legacy behavior, or a build that fails and says what to change.
- As little code as possible: DENY rules are derived directly from the legacy registration rules, without simulating either mesh.

**Non-Goals:**
- Reproducing the Envoy string-prefix behavior of legacy prefixes without variables (legacy `/api/users` also matched `/api/usersX`).
- Routes of other services on the same gateway, and the gateway's own `/` fallback route. The plugin sends every rule to the single backend (`backendRefVal`:`servicePort`), and annotations carry no header matchers, so the migration document's "different cluster" and "header matcher" overlaps (routes 3 and 4 of its example) can't come from this plugin.
- Fixing existing scanner deviations from legacy path resolution. For example, `buildClassGatewayRoutes` uses only the first class mapping.
- Modeling facade or composite gateways. Istio has none of these gateways; the routes become service-bound rules (D9).
- A catch-all rule on the service-bound HTTPRoute (user decision).
- Generating a `VirtualService` fallback. Conflicts are reported, and any fallback is manual.
- A switch to turn errors into warnings. The opt-in `autoGenerateAuthorizationPolicies` (D5) fixes the largest class of errors instead of hiding them.
- Checks for problems the legacy runtime already rejects (the same gateway path with different service paths on one gateway, `hosts` with a border gateway name), and for legacy precedence between patterns of the same length (the plugin takes the forbidden one). The first one still fails the build, as a conflict (D3).

## Decisions

### D1. Pipeline: scan → render routes → render policies → fail or write
`GenerateRoutesMojo.execute()`:

1. `RouteScanner.collect(reactorProjects)` returns `Declarations(Set<HttpRoute> routes, Set<ForbiddenPath> forbidden, List<String> errors)`. `HttpRoute` doesn't change. `ForbiddenPath` is a gateway path and a set of border gateways (`HttpRoute.Type` PUBLIC, PRIVATE, INTERNAL). `collectRoutes` stays for `GenerateRoutesMojoTest`.
2. `HttpRouteRenderer.generateHttpRoutesYaml(servicePort, routes, problems)` plans and renders the HTTPRoutes (D2, D3, D9).
3. `AuthorizationPolicyRenderer.generateAuthorizationPoliciesYaml(routes, forbidden, problems)` computes and renders the DENY rules (D4–D7).
4. `Problems(errors, warnings)` collects the messages of all steps. The warnings are logged. If there is any error, every error is logged, and the build fails with `MojoFailureException("<n> route migration errors, see log")`. Nothing is written.
5. Otherwise the HTTPRoutes and then the policies are written into one file, in the existing header and guard. Only the file I/O is wrapped in `MojoExecutionException`.

`MojoFailureException` is used because the failure is caused by the project's configuration, not by the plugin crashing.

The gateways are identified by `HttpRoute.Type`. With the enum order INTERNAL, PRIVATE, PUBLIC, a border route is exposed on gateway `G` iff `type.compareTo(G) >= 0`. Legacy (`RouteTransformer.resolveIsRouteAllowed`) registered every border route on all three gateways, with `allowed=false` on the gateways wider than its type. The legacy winner for a request is the longest matching pattern (source text length).

### D2. Cut and rewrite derivation
`cut(path)` = the part of `path` before the segment containing the first `{`, stripped of its trailing slash, or `/` if that is empty. A path without variables only loses its trailing slash (`/api/items/` → `/api/items`), because legacy matches `/x/` and `/x` the same way and Gateway API ignores a trailing slash in `PathPrefix`. Without this, `@GetMapping("/")` and `@GetMapping("/{id}")` in a controller mapped to `/api/items` would give two `PathPrefix` rules that match the same requests. `match = cut(gatewayPath)`. If `cut(servicePath)` differs from the match, `rewrite = ReplacePrefixMatch cut(servicePath)`.

Example: `/api/v1/{id}/items` → `/items/{id}/items` gives match `/api/v1` and rewrite `/items`.

This is correct only when the part after the cut is the same in both paths (variables compared by position), because `ReplacePrefixMatch` keeps everything after the match unchanged. That holds for the routes services declare in practice, so the plugin doesn't check it (user decision).

### D3. Grouping, merging and the Exact split
Border routes are grouped by their match `P` across all route types (facade and composite routes are grouped separately, D9). Grouping across types gives the same result as grouping per gateway, because exposure sets are nested chains (INTERNAL ⊂ PRIVATE ⊂ PUBLIC). For every group:

1. If all routes have the same rewrite, emit one `PathPrefix P` rule with the largest timeout. If the timeouts differ, log a warning.
2. Else, if exactly one route `S` has no variables (so its gateway path is `P` or `P/`), at least one other route has the gateway path `P/{var}`, and all routes other than `S` share one rewrite: split `S` into `Exact P` and `Exact P/` rules with `ReplaceFullPath cut(S.path)` (and `+ "/"`), or with no filter when `S` has no rewrite. The rest are merged into the `PathPrefix P` rule as in step 1.
3. Otherwise, report an error that names every route of the group with its type and rewrite, and emit nothing for `P`.

The Exact split is the same-service form of the document's limitation 2. It is safe on every gateway where `S` appears: in legacy, the `P/{var}` route exists on every border gateway (allowed or forbidden), and because it is longer it takes every request below `P/`. So `S` only ever received `P` and `P/`.

A `PathPrefix` rule goes into the HTTPRoute of the widest type in its group; the `Exact` rules go into the HTTPRoute of `S`'s type. This removes identical matches spread across several HTTPRoute resources, where Istio would pick one by creation time. On the wider gateways, the narrower routes of the group are then routed where legacy forbade them; D4 covers them with DENY rules.

Legacy routes a request by the longest matching gateway path, Istio by `Exact` and then the longest `PathPrefix`. So where a longer route with a variable competed with a shorter literal one, the literal route now wins: `/api/items/count` goes to the `/api/items/count` route, not to `/api/items/{itemId}` (19 characters against 16). This is accepted and not checked (user decision): the literal route is the more specific one, as in Spring's own mapping.

Within each HTTPRoute, rules are sorted by path specificity (more segments, then longer, then lexical), and `Exact` before `PathPrefix` with the same value.

### D4. Which paths need DENY rules
For each border gateway `G`, with gateway paths (not cut):

- `allowed` = the routes exposed on `G`;
- `implicit` = the routes not exposed on `G`, except those whose template (D6) equals an allowed one (legacy prefers `allowed=true` for the same pattern);
- `explicit` = the `@ForbiddenRoute` paths that list `G`.

A path `F` is **needed** on `G` when Istio routes it and legacy didn't:

- (a) `F ∈ implicit`, and some `P = cut(Q)`, `Q ∈ allowed`, covers `F`: `P` has no more segments than `F`, and they overlap (D6). These are the legacy implicit forbidden routes (limitations 1 and 3 of the document);
- (b) `F = cut(Q)` for a `Q ∈ allowed` with a variable, and the longest pattern of `allowed ∪ implicit` that covers `F` is not allowed, or there is none. This is the cut exposure (limitation 4): when a shorter allowed route covers `P`, legacy already routed the subtree.

Every rule path `F` (explicit or automatic) is probed against legacy: the probes are `sample(F, F)` and `sample(F, Q)` for every `Q ∈ allowed` that overlaps `F`, where `sample` builds a request path matching both, filling variables with the literal of the other path or with `~`. For a probe the rule denies (not covered by a longer `notPaths` route, D6) whose legacy route is allowed, that route is added to the `notPaths` of the rule, so the rule denies nothing legacy routed on `G`. The legacy route of a request is the longest route of `allowed ∪ implicit` that covers it, the implicit one on a tie, as in (b). This keeps legacy behavior for `@ForbiddenRoute` on a path below a shorter exposed route (`/api/v1/svc/admin` below `/api/v1/svc`: the rule then denies nothing), on a pattern that overlaps a deeper but shorter exposed route (`/api/{version}` over `/api/v1/x/y`), and for automatic rules next to a catch-all route. The probes are samples, one per overlapping route: they cover the declarations services use, but a mix of several overlapping variable routes can still hide a denied request between them.

This computation replaces a differential validation. It doesn't detect timeout precedence changes between a short route and a longer route in the same group, and it accepts literal-vs-variable precedence changes (D3).

### D5. `@ForbiddenRoute` and `autoGenerateAuthorizationPolicies`
The rules of `G` are built for `explicit`, plus:

- with `autoGenerateAuthorizationPolicies` = `true`: every needed path;
- with `false` (default): nothing more. Every needed path whose template has no explicit rule on `G` is an error. The errors are grouped per path and list all its gateways, the reason, and both fixes: `add @ForbiddenRoute({PUBLIC, PRIVATE}) to the element mapped to <path>, or set autoGenerateAuthorizationPolicies to generate the DENY rules`.

*Default `false`*: generated DENY rules on shared border gateways are a security-relevant change that service owners should opt into explicitly.

*Alternative*: always generate the rules and drop `@ForbiddenRoute`. Rejected per user decision: explicit annotations stay the default, and automation is opt-in.

### D6. DENY rule construction
- `template(p)` replaces each whole-segment variable with `{*}` and drops the trailing slash.
- `overlaps(F, Q)` compares the segments up to the shorter of the two paths: two variables always overlap, a variable and a literal overlap if the variable's segment pattern matches the literal, and two literals must be equal.
- For each rule path `F`: `paths = [F', F' + "/{**}"]` (`/` → `["/", "/{**}"]`), and `notPaths = [Q', Q' + "/{**}"]` for every `Q ∈ allowed` with `len(Q) > len(F)` and `overlaps(F, Q)`, and for every allowed legacy route of a denied probe (D4). Forbidden `/a/lit/x` with allowed `/a/{id}/x/y` needs `/a/{*}/x/y` in `notPaths`, or the rule denies `/a/lit/x/y`, which legacy routes.
- One rule per path, deduplicated by template. `notPaths` belongs to a specific path, and Istio ORs rules within a policy, so nested forbidden paths work without extra handling.
- If `F` or any `Q` of its `notPaths` has a variable that takes up only part of a segment (`{name}.txt`) or a `*` wildcard, `{*}` can't express it: that is an error, and no rule is generated for it.

### D7. AuthorizationPolicy rendering
Jackson records, like `HTTPRouteResource`: `security.istio.io/v1`, `kind: AuthorizationPolicy`, `spec.targetRefs`, `spec.action: DENY`, `spec.rules[].to[].operation.{ports, paths, notPaths}`. One policy per gateway with rules, named `{{ .Values.SERVICE_NAME }}-java-annotations-deny-<public|private|internal>`, in the order public, private, internal, targeting Gateway `public-gateway`, Gateway `private-gateway` or Service `internal-gateway-service`. `ports` is always `["8080"]`, the listener port of every border gateway (user decision). Labels come from `HttpRouteRenderer.buildRouteLabels`.

### D8. `@ForbiddenRoute` lives in route-registration-common
`ForbiddenRoute.java` next to `Route.java`: `@Retention(RUNTIME)`, `@Target({TYPE, METHOD})`, `@Documented`, `RouteType[] value()` with no default. Retention must be RUNTIME because `RouteScanner` uses `disableRuntimeInvisibleAnnotations()`. The legacy runtime ignores it. The scanner resolves its gateway path like the gateway path of a route on the same element, so it works on elements without `@Route`. An empty value or `FACADE` is an error.

*Alternative*: a separate annotations artifact owned by the plugin. Rejected per user decision: services already depend on route-registration-common.

### D9. Legacy annotation forms and the service-bound HTTPRoute
The scanner reads `@Route`, the `@Routes` container, `@FacadeRoute` and `@ForbiddenRoute`, at class or method level. A method-level route list replaces the class-level list, as in legacy. Each entry becomes one route per target:

- `gateways` empty, type PUBLIC/PRIVATE/INTERNAL: a border route of that type, with the gateway path from `@Gateway`/`@GatewayRequestMapping`;
- `gateways` empty, type FACADE: a **facade** route, with the gateway path from `@FacadeGateway`/`@FacadeGatewayRequestMapping`, or the service path when there is none;
- every name in `gateways`: `public-gateway-service`, `private-gateway-service` and `internal-gateway-service` give a border route of that type; any other name gives a **composite** route, with `@Gateway` paths.

Facade and composite routes are `HttpRoute`s with `Type.FACADE`, and `hosts` are ignored. They are planned with D2 and D3 as one more group set:

- if no rule has a rewrite, no service-bound HTTPRoute is generated (Istio sends every request for the Service to it unchanged), and if any route has a timeout, a warning says that the timeouts are not applied;
- otherwise one HTTPRoute `{{ .Values.SERVICE_NAME }}-java-annotations-facade` with parentRef Service `{{ .Values.SERVICE_NAME }}` contains **every** rule, including those without a rewrite. Once an HTTPRoute is bound to a Service, Istio returns 404 for requests that no rule matches. No catch-all rule is added (user decision).

Two legacy facade or composite routes with the same gateway path and different service paths were valid on separate legacy gateways; here they are a D3 conflict. No DENY rules are generated for facade or composite routes.

## Risks / Trade-offs

- [Many existing services start failing the build (implicit 404s, cut exposure, conflicts)] → This is intentional (they are insecure or silently wrong today). Every error names the `@ForbiddenRoute` to add or offers `autoGenerateAuthorizationPolicies`. The plugin is released as a new major version with a README migration section.
- [A DENY rule on a shared border gateway also blocks another service's routes under the same path] → Gateway paths are service-namespaced (`/api/<version>/<service>/...`), and the plugin doesn't model other services. The README states it.
- [The direct computation misses a case that a simulation would catch] → The gaps are listed in D4. Every example of the migration document that the plugin can produce has a unit test.
- [While the service-bound HTTPRoute exists, requests to the Service on other paths return 404] → The README states it and names the fix: declare those endpoints as facade routes, or call them through a border gateway.
- [Facade routes that used `@Gateway` paths change their gateway path] → This is the legacy behavior, and the README names it.
- [403 instead of 404 for forbidden paths, and path normalization] → Documented in the README as BWC notes.
- [A gateway listener not on port 8080 isn't covered by the DENY rules] → All border gateways listen on 8080 (user decision).
- [Two-module change (plugin + route-registration-common)] → The plugin pom already tracks the monorepo SNAPSHOT, so both are released in the same train.

## Migration Plan

1. Add `@ForbiddenRoute` to route-registration-common and build it (SNAPSHOT).
2. Release the plugin as a new major version.
3. Consumers fix build failures by adding `@ForbiddenRoute`, setting `autoGenerateAuthorizationPolicies` to `true`, or aligning paths.

## Open Questions

None.
