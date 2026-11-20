# Design

## Context

For motivation, see `proposal.md` (Why). The chosen approach: every route becomes a `PathPrefix` match cut at its first variable, and the paths that legacy didn't route but the cut does are denied by `AuthorizationPolicy` DENY rules. Unless stated otherwise, plugin paths and classes below are in `core-maven-plugins/httproutes-generator-maven-plugin`. Current state of the plugin:

- `RouteScanner` builds a flat `Set<HttpRoute>` (`path`, `gatewayPath`, `type`, `timeout`) from `@Route`, Spring/JAX-RS mappings, `@Gateway` and `@GatewayRequestMapping`. It does not keep where each route came from. It skips classes that have no `@Route`. It never reads the `@Routes` container, `@Route` `gateways`/`hosts`, `@FacadeRoute`, `@FacadeGateway` or `@FacadeGatewayRequestMapping`. For `@Route(RouteType.FACADE)` it takes the gateway path from `@Gateway`/`@GatewayRequestMapping`.
- `HttpRouteRenderer` groups routes by `Type` into one HTTPRoute per type. The parentRefs follow the exposure chain: PUBLIC → public-gateway, private-gateway and Service internal-gateway-service; PRIVATE → private-gateway and internal-gateway-service; INTERNAL → internal-gateway-service; FACADE → Service `{{ .Values.SERVICE_NAME }}`. It turns `{var}` into `([^/]+)` inside a `RegularExpression` match. It always uses `ReplacePrefixMatch` with the full service path, which is invalid for regex. FACADE routes whose `path == gatewayPath` are skipped, even when the FACADE HTTPRoute exists, so Istio returns 404 for them.
- Legacy facade and composite routes (`ClassRoutesSet`): both have `RouteType.FACADE`. A facade route (`@Route(RouteType.FACADE)` or `@FacadeRoute` without `gateways`) goes to the facade gateway named after the microservice, and its gateway path comes from `@FacadeGateway`/`@FacadeGatewayRequestMapping`, or equals the service path when there is none. A composite route (`gateways` with any name other than a border gateway name) goes to that gateway, with the gateway path from `@Gateway`/`@GatewayRequestMapping` and optional `hosts`. A border name in `gateways` gives a border route of that type (`RouteType.fromGatewayName`). A method-level `@Route` list replaces the class-level one.
- Legacy registration (`route-registration-common` `RouteTransformer`) registers every border route on all three border gateways, with `allowed=false` on gateways wider than the route type. It prefers `allowed=true` when a gateway path is registered twice. Annotations carry no header matchers. `HttpRouteRenderer` puts the same single `backendRefs` entry (`backendRefVal`:`servicePort`) on every rule. So route "behavior" is only forbidden vs allowed, the upstream path (rewrite) and the timeout. So a route of this plugin can't overlap another of its routes with a different cluster or a header matcher.
- The plugin depends on `route-registration-common` `7.5.2-SNAPSHOT` from the same monorepo (`core-rest-libraries/route-registration/route-registration-common`).

## Goals / Non-Goals

**Goals:**
- Generated config that keeps the legacy behavior, or a build that fails and says what to change.
- As little code as possible: DENY rules are derived directly from the legacy registration rules, without simulating either mesh.

**Non-Goals:**
- Reproducing the Envoy string-prefix behavior of legacy prefixes without variables (legacy `/api/users` also matched `/api/usersX`).
- Routes of other services on the same gateway, and the gateway's own `/` fallback route. The plugin sends every rule to the single backend (`backendRefVal`:`servicePort`), and annotations carry no header matchers, so overlaps with a different cluster or a header matcher can't come from this plugin.
- Fixing existing scanner deviations from legacy path resolution. For example, `buildClassGatewayRoutes` uses only the first class mapping.
- Modeling facade or composite gateways. Istio has none of these gateways; the routes become service-bound rules (D9).
- A catch-all rule on the service-bound HTTPRoute (user decision).
- Generating a `VirtualService` fallback, or comparing the rewrites of routes cut to one match: in practice they share one rewrite (user decision, D3).
- DENY rules on the internal gateway. Exposure there isn't a risk, as with facade and composite routes (user decision, D4).
- A switch to turn errors into warnings. The opt-in `autoGenerateAuthorizationPolicies` (D5) fixes the largest class of errors instead of hiding them.
- Checks for problems the legacy runtime already rejects (the same gateway path with different service paths on one gateway, `hosts` with a border gateway name). The first one is merged like any other group (D3).
- Legacy precedence between different patterns of the same length. The plugin takes the forbidden one, so a route allowed on a gateway is denied there when a forbidden route of the same length covers it and a DENY rule for that forbidden route is generated (user decision, D4). Otherwise the allowed route stays routed, as with the precedence change (D3): a PUBLIC `/a/a` next to an INTERNAL `/{v}` gets no rule, because no cut of an allowed route covers `/{v}`.
- A check for short matches. A variable before the service segment cuts the match to a prefix that other services share, such as `/api` or `/`, and the DENY rules for that match apply to their paths too (user decision, D2).

## Decisions

### D1. Pipeline: scan → render routes → render policies → fail or write
`GenerateRoutesMojo.execute()`:

1. `RouteScanner.collect(reactorProjects)` returns `Declarations(Set<HttpRoute> routes, Set<ForbiddenPath> forbidden, List<String> errors)`. `HttpRoute` doesn't change. `ForbiddenPath` is a gateway path and a set of external gateways (`HttpRoute.Type` PUBLIC, PRIVATE). `collectRoutes` stays for `GenerateRoutesMojoTest`.
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

A variable before the service segment gives a short match that other services share: `/api/{version}/svc/items` gives `/api`, and `/{tenant}/items` gives `/`. The rule then catches every request under that match that no longer rule on the gateway matches. The cut exposure (D4 (b)) needs a DENY rule for the match, and that rule denies every path under it except the routes of this service, other services' paths included, because DENY rules apply before routing. This is expected, and the plugin doesn't check the length of a match (user decision).

### D3. Grouping and merging
Border routes are grouped by their match `P` across all route types (facade and composite routes are grouped separately, D9). Grouping across types gives the same result as grouping per gateway, because exposure sets are nested chains (INTERNAL ⊂ PRIVATE ⊂ PUBLIC). Every group becomes one `PathPrefix P` rule:

- The rewrite is that of the first route, with the routes sorted by gateway path, service path, type and timeout. The rewrites aren't compared: routes cut to one match share one rewrite in practice (user decision).
- The timeout is the largest one, where a route without a timeout counts as the 2-minute gateway default. When the default wins, the rule has no timeout. If the timeouts differ, a warning is logged.

The rule goes into the HTTPRoute of the widest type in its group. This removes identical matches spread across several HTTPRoute resources, where Istio would pick one by creation time. On the wider gateways, the narrower routes of the group are then routed where legacy forbade them; D4 covers them with DENY rules.

Legacy routes a request by the longest matching gateway path, Istio by the longest `PathPrefix`. So where a longer route with a variable competed with a shorter literal one, the literal route now wins: `/api/items/count` goes to the `/api/items/count` route, not to `/api/items/{itemId}` (19 characters against 16). This is accepted and not checked (user decision): the literal route is the more specific one, as in Spring's own mapping.

Within each HTTPRoute, rules are sorted by path specificity (more segments, then longer, then lexical).

### D4. Which paths need DENY rules
The internal gateway needs no DENY rules (user decision). For each of the public and private gateways `G`, with gateway paths (not cut):

- `allowed` = the routes exposed on `G`;
- `implicit` = the routes not exposed on `G`, except those whose template (D6) equals an allowed one (legacy prefers `allowed=true` for the same pattern);
- `explicit` = the `@ForbiddenRoute` paths that list `G`.

A path `F` is **needed** on `G` when Istio routes it and legacy didn't:

- (a) `F ∈ implicit`, and some `P = cut(Q)`, `Q ∈ allowed`, covers `F`: `P` has no more segments than `F`, and they overlap (D6). This doesn't apply when legacy routes `F` by a longer allowed route, that is when the legacy route of `sample(F, F)` (below) is allowed. These are the legacy implicit forbidden routes (`allowed=false`, 404);
- (b) `F = cut(Q)` for a `Q ∈ allowed` with a variable, and the longest pattern of `allowed ∪ implicit` that covers `F` is not allowed, or there is none. This is the cut exposure: legacy routed only the shape of `Q`, and the cut routes the whole subtree below `F`. When a shorter allowed route covers `F`, legacy already routed the subtree.

Every rule path `F` (explicit or automatic) is probed against legacy: the probes are `sample(F, F)` and `sample(F, Q)` for every `Q ∈ allowed` that overlaps `F`, where `sample` builds a request path matching both, filling variables with the literal of the other path or with `~`. For a probe whose legacy route is allowed, that route is added to the `notPaths` of the rule, so the rule denies nothing legacy routed on `G`. The legacy route of a request is the longest route of `allowed ∪ implicit` that covers it, the implicit one on a tie, as in (b). This keeps legacy behavior for a pattern that overlaps a deeper but shorter exposed route (`/api/{version}` over `/api/v1/x/y`), and for automatic rules next to a catch-all route. An explicit rule is the exception: the routes that cover `sample(F, F)` are not added, so that the rule denies its own path even where legacy routed it by a shorter exposed route. `@ForbiddenRoute` on `/api/v1/svc/admin` below the exposed `/api/v1/svc` denies `/api/v1/svc/admin`, and the rest of `/api/v1/svc` stays routed. The probes are samples, one per overlapping route: they cover the declarations services use, but a mix of several overlapping variable routes can still hide a denied request between them. Taking the implicit route on a tie is expected (user decision): below a PUBLIC route `R`, the automatic rule for an INTERNAL `R/{id}` doesn't exclude a PUBLIC `R/list` of the same length, so `R/list` is denied on the public and private gateways, while next to a shorter INTERNAL `R/{i}` it is excluded and stays routed.

This computation replaces a differential validation. It doesn't detect timeout precedence changes between a short route and a longer route in the same group, and it accepts literal-vs-variable precedence changes (D3).

### D5. `@ForbiddenRoute` and `autoGenerateAuthorizationPolicies`
The rules of `G` are built for `explicit`, plus:

- with `autoGenerateAuthorizationPolicies` = `true`: every needed path;
- with `false` (default): nothing more. Every needed path whose template has no explicit rule on `G` is an error. The errors are grouped per path and list all its gateways, the reason, and the fix: `add @ForbiddenRoute({PUBLIC, PRIVATE}) to the element mapped to <path>`. They don't mention `autoGenerateAuthorizationPolicies`.

*Default `false`*: generated DENY rules on the shared public and private gateways are a security-relevant change that service owners should opt into explicitly.

*Alternative*: always generate the rules and drop `@ForbiddenRoute`. Rejected per user decision: explicit annotations stay the default, and automation is opt-in.

### D6. DENY rule construction
- `template(p)` replaces each whole-segment variable with `{*}` and drops the trailing slash.
- `overlaps(F, Q)` compares the segments up to the shorter of the two paths: two variables always overlap, a variable and a literal overlap if the variable's segment pattern matches the literal, and two literals must be equal.
- For each rule path `F`: `paths = [F', F' + "/{**}"]` (`/` → `["/", "/{**}"]`), and `notPaths = [Q', Q' + "/{**}"]` for every `Q ∈ allowed` with `len(Q) > len(F)` and `overlaps(F, Q)`, and for every allowed legacy route of a denied probe (D4). Forbidden `/a/lit/x` with allowed `/a/{id}/x/y` needs `/a/{*}/x/y` in `notPaths`, or the rule denies `/a/lit/x/y`, which legacy routes.
- One rule per path, deduplicated by template. `notPaths` belongs to a specific path, and Istio ORs rules within a policy, so nested forbidden paths work without extra handling.
- If `F` or any `Q` of its `notPaths` has a variable that takes up only part of a segment (`{name}.txt`), `{*}` can't express it: that is an error, and no rule is generated for it.

### D7. AuthorizationPolicy rendering
Jackson records, like `HTTPRouteResource`: `security.istio.io/v1`, `kind: AuthorizationPolicy`, `spec.targetRefs`, `spec.action: DENY`, `spec.rules[].to[].operation.{ports, paths, notPaths}`. One policy per gateway with rules, named `{{ .Values.SERVICE_NAME }}-java-annotations-deny-<public|private>`, in the order public, private, targeting Gateway `public-gateway` or Gateway `private-gateway`. `ports` is always `["8080"]`, the listener port of both gateways (user decision). Labels come from `HttpRouteRenderer.buildRouteLabels`.

### D8. `@ForbiddenRoute` lives in route-registration-common
`ForbiddenRoute.java` next to `Route.java`: `@Retention(RUNTIME)`, `@Target({TYPE, METHOD})`, `@Documented`, `RouteType[] value()` with no default. Retention must be RUNTIME because `RouteScanner` uses `disableRuntimeInvisibleAnnotations()`. The legacy runtime ignores it. The scanner resolves its gateway path like the gateway path of a route on the same element, so it works on elements without `@Route`. An empty value, or a value other than `PUBLIC` and `PRIVATE`, is an error.

*Alternative*: a separate annotations artifact owned by the plugin. Rejected per user decision: services already depend on route-registration-common.

### D9. Legacy annotation forms and the service-bound HTTPRoute
The scanner reads `@Route`, the `@Routes` container, `@FacadeRoute` and `@ForbiddenRoute`, at class or method level. A method-level route list replaces the class-level list, as in legacy, so a method `@Route` without a type is INTERNAL, whatever the class `@Route` says. The type of a route is its `value`, or its `type` when `value` is INTERNAL, as in legacy (`RouteAnnotationUtils`). Each entry becomes one route per target:

- `gateways` empty, type PUBLIC/PRIVATE/INTERNAL: a border route of that type, with the gateway path from `@Gateway`/`@GatewayRequestMapping`;
- `gateways` empty, type FACADE: a **facade** route, with the gateway path from `@FacadeGateway`/`@FacadeGatewayRequestMapping`, or the service path when there is none;
- every name in `gateways`: `public-gateway-service`, `private-gateway-service` and `internal-gateway-service` give a border route of that type; any other name gives a **composite** route, with `@Gateway` paths.

Facade and composite routes are `HttpRoute`s with `Type.FACADE`, and `hosts` are ignored. They are planned with D2 and D3 as one more group set:

- if no rule has a rewrite, no service-bound HTTPRoute is generated (Istio sends every request for the Service to it unchanged), and if any route has a timeout, a warning says that the timeouts are not applied;
- otherwise one HTTPRoute `{{ .Values.SERVICE_NAME }}-java-annotations-facade` with parentRef Service `{{ .Values.SERVICE_NAME }}` contains **every** rule, including those without a rewrite. Once an HTTPRoute is bound to a Service, Istio returns 404 for requests that no rule matches. No catch-all rule is added (user decision).

Two legacy facade or composite routes with the same gateway path and different service paths were valid on separate legacy gateways; here they are merged into one rule with the rewrite of the first one (D3), and this isn't checked. No DENY rules are generated for facade or composite routes.

## Risks / Trade-offs

- [Many existing services start failing the build (implicit 404s, cut exposure)] → This is intentional (they are insecure or silently wrong today). Every error names the `@ForbiddenRoute` to add. The plugin is released as a new major version with a README migration section.
- [A DENY rule on the shared public or private gateway also blocks another service's routes under the same path] → Gateway paths are service-namespaced (`/api/<version>/<service>/...`), and the plugin doesn't model other services. The README states it. A variable before the service segment cuts the match to a shared prefix (`/api`, `/`), and its DENY rule then blocks the other services' paths under it. This is expected and not checked (user decision, D2).
- [An allowed route is denied where a forbidden route of the same length covers it and a DENY rule for that forbidden route is generated] → Expected (user decision, D4). The legacy order of patterns of the same length isn't modeled, and the plugin takes the forbidden one. Without such a rule, the allowed route stays routed (D3).
- [The direct computation misses a case that a simulation would catch] → The gaps are listed in D4. Every case of D4 has a unit test, and an end-to-end test covers a typical controller set.
- [While the service-bound HTTPRoute exists, requests to the Service on other paths return 404] → The README states it and names the fix: declare those endpoints as facade routes, or call them through a border gateway.
- [Facade routes that used `@Gateway` paths change their gateway path] → This is the legacy behavior, and the README names it.
- [403 instead of 404 for forbidden paths, and path normalization] → Documented in the README as BWC notes.
- [A gateway listener not on port 8080 isn't covered by the DENY rules] → The public and private gateways listen on 8080 (user decision).
- [A merged rule where the 2-minute default timeout wins has no timeout, so it relies on the gateway request timeout] → Istio sets no request timeout by default, so the platform's mesh configuration must set it to 2 minutes.
- [Two-module change (plugin + route-registration-common)] → The plugin pom already tracks the monorepo SNAPSHOT, so both are released in the same train.

## Migration Plan

1. Add `@ForbiddenRoute` to route-registration-common and build it (SNAPSHOT).
2. Release the plugin as a new major version.
3. Consumers fix build failures by adding `@ForbiddenRoute`, setting `autoGenerateAuthorizationPolicies` to `true`, or aligning paths.

## Open Questions

None.
