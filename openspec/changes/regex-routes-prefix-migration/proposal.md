# Proposal

## Why

The plugin is supposed to reproduce legacy Cloud-Core Service Mesh routing in Istio, but it doesn't when a route has path variables. Such a route becomes a `RegularExpression` match today. Istio always ranks regex below `PathPrefix`, and `ReplacePrefixMatch` does not work with regex, so these routes lose their legacy precedence and their rewrite (see `core-maven-plugins/httproutes-generator-maven-plugin/regex-routes-migration.md`). The plugin also drops the `allowed: false` (404) routes that the legacy library created automatically on wider gateways. As a result, a shorter `PUBLIC` prefix on the public gateway serves an `INTERNAL` endpoint that sits below it. Facade routes are also broken: the plugin reads their gateway paths from the wrong annotation, ignores composite routes, and drops facade routes without a rewrite even when the service already has an HTTPRoute, so Istio returns 404 for them. Services are being migrated now, so we need generated config that is correct or a build that fails, not a "MANUAL REVIEW REQUIRED" comment.

## What Changes

- **BREAKING** Every generated HTTPRoute path match is `PathPrefix` or `Exact`. `RegularExpression` matches and the `MANUAL REVIEW REQUIRED` comment are removed.
- A gateway path with path variables is cut at its first variable and becomes a `PathPrefix` match. The prefix rewrite is cut the same way: the service path is cut at its first variable. The plugin assumes that the part after the cut is the same in both paths, as it is in practice, and doesn't check it.
- Routes that collapse to the same prefix on the same gateway are merged when their rewrite is the same. If only their timeouts differ, the merged rule gets the largest timeout and the plugin logs a warning.
- A shorter route that legacy only reached through its bare root path (because a `<root>/{var}` route took everything below it) is emitted as two `Exact` matches on `<root>` and `<root>/`, with a `ReplaceFullPath` rewrite. This keeps legacy precedence when the two routes differ in rewrite or timeout. The migration document uses `Exact` for a header-matched root route to another service. Here it is used only for two routes of the same service, because the plugin can't produce the other case.
- The scanner reads all legacy route annotation forms: the repeatable `@Routes` container (two or more `@Route` on one element), `@Route(gateways = ...)`, `@FacadeRoute`, and facade gateway paths from `@FacadeGateway` / `@FacadeGatewayRequestMapping`. Today it silently skips them. A border gateway name in `gateways` (`public-gateway-service`, `private-gateway-service`, `internal-gateway-service`) gives a route of that type, as in legacy. Every other name in `gateways` is a composite gateway.
- **BREAKING** Facade and composite routes. Istio has no facade or composite gateway, so the plugin does not model them as gateways and does not compare them with legacy routing. Instead:
  - they become rules of one HTTPRoute bound to the service's own k8s Service (`{{ .Values.SERVICE_NAME }}`), with the same cut, merge and `Exact` split as on gateways;
  - this HTTPRoute is generated only when at least one facade or composite route has a rewrite. Without a rewrite, Istio already routes the request to the service unchanged;
  - when it is generated, it contains **all** facade and composite routes of the service, including those without a rewrite. Once any HTTPRoute is bound to a Service, Istio returns 404 for every request its rules don't match. There is no catch-all rule, so other paths called through the Service also return 404 while this HTTPRoute exists;
  - facade gateway paths come from `@FacadeGateway` / `@FacadeGatewayRequestMapping`, as in legacy. Today the plugin uses `@Gateway` / `@GatewayRequestMapping` for them. Composite gateway paths come from `@Gateway` / `@GatewayRequestMapping`, as in legacy. Composite `hosts` are ignored.
- New checks, run before any file is written. The plugin derives the legacy behavior directly from the legacy registration rules (every route is registered on all border gateways, forbidden on those wider than its type, and the longest pattern wins) and **fails the build** with an actionable error when:
  - routes with different rewrites collide after the cut (conflict). This also applies to facade and composite routes that end up in the same service-bound rule;
  - Istio would route a path that legacy returned 404 for or did not route at all, and no DENY rule covers it. This covers the legacy implicit forbidden routes (a narrower-type route below a wider-type prefix) and the exposure caused by the cut;
  - a DENY rule can't be expressed as an Istio path template (a partial-segment variable or a `*` wildcard);
  - `@ForbiddenRoute` is invalid or forbids the path of a route exposed on the same gateway.
- New annotation `@ForbiddenRoute` in `route-registration-common` marks a class or method gateway path as forbidden on the listed border gateways.
- New output: an Istio `AuthorizationPolicy` (`action: DENY`) per target gateway. It uses `{*}`/`{**}` path templates, `notPaths` for longer allowed routes below the forbidden path, and port `8080`, the listener port of every border gateway. By default it is built from `@ForbiddenRoute` only, and no policy is generated when no `@ForbiddenRoute` is present.
- New plugin parameter `autoGenerateAuthorizationPolicies` (boolean, default `false`), set in the plugin `<configuration>` in `pom.xml`. When `true`, the plugin also generates DENY rules without explicit annotations, as Option 1 in the migration document describes:
  - for every legacy implicit forbidden route (a narrower-type route on a wider gateway) that the generated HTTPRoutes would otherwise route on that gateway;
  - for every controller subtree that the cut exposes and that no shorter allowed route covered in legacy.

  When `false`, these cases fail the build, and the error suggests either `@ForbiddenRoute` or enabling the parameter.
- The plugin never generates `VirtualService` or `EnvoyFilter`.
- Overlaps with routes of **another service** are out of scope. The plugin sends every rule to the single backend (`backendRefVal`:`servicePort`), and the annotations have no header matchers. So a plugin-generated route can't overlap a route with a different cluster or a header-matched route, like the `migrated-api` → `another-service` and `:method: GET` → `another-service` routes in the migration document. Route behavior is only forbidden/allowed, the rewrite and the timeout.
- **BREAKING** Existing services can now fail the build:
  - with `autoGenerateAuthorizationPolicies` disabled, those with a narrower-type method under a wider-type class prefix, or with variable routes that the cut widens;
  - regardless of the parameter, those with rewrite conflicts or forbidden paths an AuthorizationPolicy can't express.

  The errors say which `@ForbiddenRoute` to add or which parameter to set. Facade routes that relied on `@Gateway` paths get the legacy gateway path instead. The plugin is released as a new major version.

## Capabilities

### New Capabilities
- `httproute-generation`: How scanned routes become HTTPRoute rules: reading the legacy annotation forms, prefix-only matching, cutting at the first path variable, deriving the rewrite, merging, the `Exact` split, the service-bound HTTPRoute for facade and composite routes, and which resource kinds the plugin may output.
- `route-migration-validation`: Finding the paths that Istio would route and legacy didn't on each border gateway, and failing the build with actionable errors.
- `forbidden-route-policies`: The `@ForbiddenRoute` annotation, the opt-in `autoGenerateAuthorizationPolicies` parameter, and how both become Istio `AuthorizationPolicy` DENY rules.

### Modified Capabilities
<!-- None: no specs exist yet under openspec/specs/. -->

## Impact

- **Plugin code**:
  - `HttpRouteRenderer`: match and rewrite generation, regex removal. The service-bound (facade) HTTPRoute keeps rules without a rewrite and is generated only when at least one rule has a rewrite.
  - `RouteScanner`: scans `@ForbiddenRoute`, `@Routes`, `@Route(gateways, hosts)`, `@FacadeRoute`, `@FacadeGateway` and `@FacadeGatewayRequestMapping`, and includes classes that carry only `@ForbiddenRoute` or `@FacadeRoute`. The `HttpRoute` record stays unchanged.
  - `GenerateRoutesMojo`: new optional parameter `autoGenerateAuthorizationPolicies` (default `false`). Fails the build with `MojoFailureException` on errors, and writes AuthorizationPolicies.
  - New `AuthorizationPolicyRenderer`, and the small `RoutePaths`, `ForbiddenPath` and `Problems` helpers.
- **Plugin module**: all plugin changes are in `core-maven-plugins/httproutes-generator-maven-plugin` (package `com.netcracker.routes.gateway.plugin`).
- **Annotation module**: `core-rest-libraries/route-registration/route-registration-common` gets the new `@ForbiddenRoute` annotation (RUNTIME retention). The legacy runtime library ignores it. The plugin already depends on `route-registration-common` `7.5.2-SNAPSHOT` from the same monorepo.
- **Generated output**: the YAML file can now also contain `security.istio.io/v1` `AuthorizationPolicy` resources inside the same Helm `SERVICE_MESH_TYPE == Istio` guard. Rules that were regex become prefix or exact rules. The facade HTTPRoute now also lists facade and composite routes without a rewrite.
- **Docs/tests**: the plugin `README.md` (new annotation, errors, AuthorizationPolicy output, the new parameter, legacy annotation forms, facade and composite routes, migration section). Unit tests for the cut, merging, the `Exact` split, the DENY rule computation, the service-bound HTTPRoute and policy rendering. Tests reuse the migration document's example only for the routes the plugin can produce (it can't produce the `another-service` routes). The existing renderer tests that expect regex output are updated. In `GenerateRoutesMojoTest`, only the facade assertion for `SpringTestController8` changes, to the legacy gateway path.
