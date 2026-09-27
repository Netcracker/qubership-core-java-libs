# Spec Delta

## Purpose

Defines how routes declared with legacy route-registration annotations are translated into Gateway API `HTTPRoute` rules. The generated rules must keep legacy "longest prefix wins" routing and path rewriting in Istio without using regular expression matches.

## ADDED Requirements

### Requirement: Allowed output resource kinds
The plugin SHALL emit only `gateway.networking.k8s.io/v1` `HTTPRoute` resources and, when forbidden routes are declared, `security.istio.io/v1` `AuthorizationPolicy` resources. The plugin MUST NOT emit `VirtualService`, `EnvoyFilter` or any other resource kind.

#### Scenario: Routes without forbidden declarations
- **WHEN** the scanned project declares routes and no forbidden routes
- **THEN** the output file contains only `HTTPRoute` resources

#### Scenario: Routes that cannot be migrated
- **WHEN** validation finds a conflict that only a `VirtualService` could express
- **THEN** the build fails, and the output contains neither a `VirtualService` nor an `EnvoyFilter`

### Requirement: Single backend for all rules
Every generated HTTPRoute rule SHALL have the same single `backendRefs` entry: the configured `backendRefVal` and `servicePort`. Route behavior for merging, the Exact split and conflict detection SHALL be only the rewrite and the timeout. The plugin SHALL NOT model or compare backends or header matchers, because annotations can't declare routes to another service or header-matched routes.

#### Scenario: Colliding routes are never split by backend
- **WHEN** several routes of the scanned project cut to the same `PathPrefix` on a gateway
- **THEN** they are merged, split into `Exact` rules or reported as a conflict based only on their rewrites and timeouts, and every resulting rule has the same `backendRefs` entry

### Requirement: Path matches are PathPrefix or Exact only
Every generated HTTPRoute rule match SHALL have path type `PathPrefix` or `Exact`. The plugin MUST NOT generate `RegularExpression` path matches. The plugin MUST NOT emit the `MANUAL REVIEW REQUIRED` comment.

#### Scenario: Route with path variable
- **WHEN** a route has gateway path `/api/v1/my-service/items/{id}`
- **THEN** the generated rule uses a `PathPrefix` match, and the output has no `RegularExpression` match and no `MANUAL REVIEW REQUIRED` comment

### Requirement: Cut gateway path at the first path variable
When a route's gateway path contains a path variable (`{...}`), the plugin SHALL cut it just before the path segment that contains the first variable. The part before the cut, without a trailing `/`, becomes the `PathPrefix` value. If nothing remains before the cut, the match value SHALL be `/`. Gateway paths without variables SHALL be used unchanged as `PathPrefix` values.

#### Scenario: Variable in the middle
- **WHEN** a route has gateway path `/api/v1/my-service/resource/{var1}/internal-api/status`
- **THEN** the rule matches `PathPrefix` `/api/v1/my-service/resource`

#### Scenario: Variable is the last segment
- **WHEN** a route has gateway path `/api/v1/my-service/order/{id}`
- **THEN** the rule matches `PathPrefix` `/api/v1/my-service/order`

#### Scenario: Variable inside a segment
- **WHEN** a route has gateway path `/api/v1/files/{name}.txt`
- **THEN** the rule matches `PathPrefix` `/api/v1/files`

#### Scenario: Path without variables
- **WHEN** a route has gateway path `/api/v1/my-service/resource`
- **THEN** the rule matches `PathPrefix` `/api/v1/my-service/resource`

### Requirement: Cut the prefix rewrite the same way
When the gateway path and service path of a route differ, the plugin SHALL generate a `URLRewrite` filter of type `ReplacePrefixMatch`. Its value SHALL be the service path cut at its first path variable, with the same rule as the gateway path. The plugin SHALL NOT check that the parts after the cut are the same in both paths. When the gateway path and service path are equal, no rewrite filter SHALL be generated.

#### Scenario: Rewrite with a path variable
- **WHEN** a route has gateway path `/api/v1/my-service/resource/{var1}/sub` and service path `/resource/{var1}/sub`
- **THEN** the rule matches `PathPrefix` `/api/v1/my-service/resource` and rewrites with `ReplacePrefixMatch` `/resource`

#### Scenario: Rewrite without path variables
- **WHEN** a route has gateway path `/api/v1/my-service/resource` and service path `/resource`
- **THEN** the rule matches `PathPrefix` `/api/v1/my-service/resource` and rewrites with `ReplacePrefixMatch` `/resource`

#### Scenario: Same paths
- **WHEN** a route has gateway path equal to service path `/items/{id}`
- **THEN** the rule matches `PathPrefix` `/items` and has no `URLRewrite` filter

### Requirement: One rule per match on a gateway
Every gateway SHALL see at most one generated rule per (path type, path value) match, counting all generated HTTPRoute resources attached to that gateway. When several routes produce the same match on the same gateway with the same rewrite, the plugin SHALL merge them into one rule. The merged rule SHALL go into the HTTPRoute resource of the widest route type among them (PUBLIC > PRIVATE > INTERNAL). If the merged routes have different timeouts and the Exact split does not apply, the merged rule SHALL use the largest timeout, and the plugin SHALL log a warning. The warning names the match, the gateways, the routes merged with their sources, and the timeout that was chosen.

#### Scenario: Collapsed routes with the same rewrite
- **WHEN** PUBLIC route `/api/v1/my-service/resource` → `/resource` and PUBLIC route `/api/v1/my-service/resource/{var1}/sub` → `/resource/{var1}/sub` are declared
- **THEN** the PUBLIC HTTPRoute contains exactly one rule matching `PathPrefix` `/api/v1/my-service/resource` with `ReplacePrefixMatch` `/resource`

#### Scenario: Same rewrite, different route types
- **WHEN** PUBLIC route `/api/a` → `/a` and INTERNAL route `/api/a/{id}/x` → `/a/{id}/x` are declared
- **THEN** only the PUBLIC HTTPRoute contains a rule for `PathPrefix` `/api/a`, and the INTERNAL HTTPRoute contains no rule for it

#### Scenario: Timeouts differ
- **WHEN** two routes collapse to `PathPrefix` `/api/a` with the same rewrite, their timeouts are 5000 ms and 60000 ms, and the Exact split does not apply
- **THEN** the merged rule has `timeouts.request: 1m`, and the build log has a warning naming both routes and the chosen timeout

### Requirement: Exact split for bare-root routes
The plugin SHALL use `Exact` matches when all of the following hold:

- a route S has gateway path P with no path variables;
- another route R cuts to the same prefix P because its gateway path is exactly `P/{var}`;
- S and R differ in rewrite or timeout. Both are routes of this service; the split does not depend on backends or header matchers.

In this case S SHALL be emitted as two rules on all of S's gateways: `Exact` P and `Exact` P + `/`. If S has a rewrite, each rule SHALL use a `ReplaceFullPath` filter, with value S's service path and S's service path + `/` respectively. Both rules keep S's timeout. R and every other route cut to P SHALL be emitted as the `PathPrefix` P rule. If the routes left in the `PathPrefix` P rule still differ in rewrite, the plugin SHALL report a conflict.

#### Scenario: Controller root with a different rewrite than the single-resource route
- **WHEN** route S `/api/v1/svc/items` → `/v1/items` and route R `/api/v1/svc/items/{id}` → `/v2/items/{id}` share a gateway
- **THEN** that gateway gets these rules: `Exact` `/api/v1/svc/items` with `ReplaceFullPath` `/v1/items`; `Exact` `/api/v1/svc/items/` with `ReplaceFullPath` `/v1/items/`; `PathPrefix` `/api/v1/svc/items` with `ReplacePrefixMatch` `/v2/items`

#### Scenario: Split does not apply because R continues after the variable
- **WHEN** route S `/api/v1/svc/items` → `/v1/items` and route R `/api/v1/svc/items/{id}/details` → `/v2/items/{id}/details` share a gateway
- **THEN** no `Exact` rules are generated, and the build fails with a conflict report for `PathPrefix` `/api/v1/svc/items`

### Requirement: Legacy route annotation forms
The plugin SHALL read routes from every legacy route annotation form, at class and method level: `@Route`, the repeatable container `@Routes`, and `@FacadeRoute`. A method-level list of route annotations SHALL replace the class-level list. Each route annotation SHALL be resolved as legacy does:

- with empty `gateways`, a PUBLIC, PRIVATE or INTERNAL type gives a border route of that type, and FACADE gives a facade route;
- each border gateway name in `gateways` (`public-gateway-service`, `private-gateway-service`, `internal-gateway-service`) gives a border route of the matching type;
- each other name in `gateways` gives a composite route.

Border and composite routes SHALL take their gateway paths from `@Gateway` / `@GatewayRequestMapping`. Facade routes SHALL take them from `@FacadeGateway` / `@FacadeGatewayRequestMapping`, or use the service path when neither is present. The `hosts` attribute SHALL be ignored.

#### Scenario: Two Route annotations on one method
- **WHEN** a method mapped to `/items` has `@Route(RouteType.PUBLIC)` and `@Route(gateways = "composite-gw")`
- **THEN** the plugin generates a PUBLIC rule for `/items` and a service-bound rule for `/items`

#### Scenario: Border gateway name in gateways
- **WHEN** a method mapped to `/items` has `@Route(gateways = "private-gateway-service")`
- **THEN** the plugin generates a PRIVATE rule for `/items`

#### Scenario: Any other gateway name is composite
- **WHEN** a method mapped to `/items` has `@Route(gateways = "my-service")` and `@GatewayRequestMapping("/api/items")`
- **THEN** the plugin generates a composite route with gateway path `/api/items`, and no border rule

#### Scenario: Facade route with only a border gateway path annotation
- **WHEN** a method mapped to `/items` has `@Route(RouteType.FACADE)` and `@GatewayRequestMapping("/api/items")`, and no facade gateway path annotation
- **THEN** the facade route has gateway path `/items`, and `/api/items` is not used for it

#### Scenario: Facade gateway path
- **WHEN** a method mapped to `/items` has `@FacadeRoute` and `@FacadeGatewayRequestMapping("/facade/items")`
- **THEN** the facade route has gateway path `/facade/items` and service path `/items`

#### Scenario: Method-level list replaces the class-level list
- **WHEN** a class mapped to `/items` has `@Route(RouteType.PUBLIC)`, and its method mapped to `/{id}` has `@Route(RouteType.INTERNAL)`
- **THEN** the method's route `/items/{id}` is only an INTERNAL route, and no PUBLIC route is generated for it

### Requirement: Service-bound HTTPRoute for facade and composite routes
Facade and composite routes SHALL NOT be attached to any gateway. They SHALL be planned as one more target with the same cut, rewrite, merge and `Exact` split rules as a gateway, and rendered as rules of one HTTPRoute named `{{ .Values.SERVICE_NAME }}-java-annotations-facade`, whose only parentRef is the Service `{{ .Values.SERVICE_NAME }}`.

The plugin SHALL generate this HTTPRoute only when at least one of its planned rules has a `URLRewrite` filter. When it is generated, it SHALL contain a rule for every facade and composite route, including routes without a rewrite. It SHALL NOT contain a catch-all rule that no route declares. When the HTTPRoute is not generated and some facade or composite route has a timeout, the plugin SHALL log a warning that these timeouts are not applied.

Two facade or composite routes with the same match and different rewrites SHALL be reported as a conflict, and the build SHALL fail.

#### Scenario: No rewrite
- **WHEN** the only facade routes are `/items` → `/items` and `/orders` → `/orders`
- **THEN** no service-bound HTTPRoute is generated

#### Scenario: One route with a rewrite
- **WHEN** facade routes `/facade/items` → `/items` and `/orders` → `/orders` are declared
- **THEN** the service-bound HTTPRoute has a `PathPrefix` `/facade/items` rule with `ReplacePrefixMatch` `/items`, a `PathPrefix` `/orders` rule with no `URLRewrite` filter, and no other rules

#### Scenario: Composite route
- **WHEN** a method mapped to `/items` has `@Route(gateways = "composite-gw", hosts = "example.com")` and `@GatewayRequestMapping("/composite/items")`
- **THEN** the service-bound HTTPRoute has a `PathPrefix` `/composite/items` rule with `ReplacePrefixMatch` `/items`, and no resource references `composite-gw` or `example.com`

#### Scenario: Facade and composite routes collide
- **WHEN** a composite route `/x` → `/a` and a facade route `/x` → `/b` are declared
- **THEN** the build fails with a conflict error that names both routes

### Requirement: Deterministic rendering
Generated resources and rules SHALL be rendered in a deterministic order, so that the same input always produces byte-identical output. Rule order SHALL keep the existing path-specificity sorting: more segments first, then longer path, then lexical order. The sort uses the generated match value, and `Exact` rules come before `PathPrefix` rules with the same value.

#### Scenario: Repeated builds
- **WHEN** the plugin runs twice on the same compiled classes
- **THEN** both output files are byte-identical

### Requirement: Output written only after successful validation
The plugin SHALL write the output file only when route migration validation reports no errors. When validation reports errors, the plugin SHALL fail the build and SHALL NOT create or overwrite the output file.

#### Scenario: Validation error
- **WHEN** validation reports at least one error
- **THEN** the Maven build fails, and the previous output file, if one exists, is left unchanged
