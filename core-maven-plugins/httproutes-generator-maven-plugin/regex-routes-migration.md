# Regex Routes Migration

## Overview

When migration routes from Legacy Cloud-Core Service Mesh to Istio ambient mesh, we must keep the exact same 100% BWC behavior of routing. But some capabilities of legacy mesh cannot be reprodced in istio easily. This document describes problem with migrating regex routes to Istio ambient mesh.

## Legacy Cloud-Core Service Mesh Behavior

### Routes With Regex

#### Control-Plane API -> Envoyproxy Configuration Mapping

In Legacy Cloud-Core Service Mesh API there were only fields `prefix` and `prefixRewrite` for matching and rewriting path in route. But these fields support specifying path variables in Spring Framework format, e.g. 

`prefix: /api/v1/my-service/{var1}/my-resource/{var2}/subresource` and 

`prefixRewrite: /api/v1/{var1}/my-resource/{var2}/subresource`. 

Regular prefixes (and prefixRewrites) without path variables are translated to prefix matchers (and prefix rewrites) in envoy.

In case prefix contains path variable, it is translated to regex matcher and regex rewrite in envoyproxy, e.g.:

```json
{
  "matcher": { "regExp": "/api/v1/my-service/([^/]+)/my-resource/([^/]+)/subresource(/.*)?" },
  "action": {
    "clusterName": "my-service||my-service||8080",
    "hostRewrite": "my-service:8080",
    "regexpRewrite": "/api/v1/\\1/my-resource/\\2/subresource\\3"
  }
}
```

Regex groups description:

- `([^/]+)` - matches single path variable;
- `(/.*)?` - emulates prefix behavior.



#### Legacy Mesh Routes Ordering

In Legacy Cloud-Core Service Mesh routes ordered in gateway based on prefix length (regex matcher also considered to be a prefix) - so the longest string among all prefix and regex matchers matches earlier and wins.

### Route Types - Public, Private and Internal Gateways Exposure Level

OOB gateways have different exposure level:

- public gateway can be exposed to the Internet;
- private gateway exposed to the backoffice network;
- internal gateway only accessible inside the k8s via k8s service.

So logically route type considered `internal` if it is exposed on internal gateway only; `private` if it is exposed on private and internal gateways; and `public` if it is exposed on all 3 gateways.

Sometimes, in microservice there is a REST controller (root resource, e.g. `/api/v1/my-service/resource`) that should be `public`. But single endpoint in it shoud be `private` or `internal`, e.g. `/api/v1/my-service/resource/{var1}/internal-api` should only be accessible via `internal` gateway.

To achive such behaviour, in Legacy Cloud-Core Service Mesh you can register route with field `allowed: false` that will create route with directResponse code 404.

A longer route can allow single endpoint back below the forbidden one: in our example `/api/v1/my-service/resource/{var1}/internal-api/status` stays `public` on all gateways. Legacy mesh picks the longest match, so this route wins over the forbidden `/api/v1/my-service/resource/{var1}/internal-api`.

Routes can also differ by header matchers only. In our example listing of resources (`GET /api/v1/my-service/resource`) is served by `another-service`, while reading a single resource (`GET /api/v1/my-service/resource/{var1}`) is still served by `my-service`. So a route with `:method` header matcher is registered on the controller root, and a route with path variable keeps single resource requests on `my-service`.

Not every controller has a route on its root. In our example only `/api/v1/my-service/order/{var1}/items` is exposed from the order controller, and every other path below `/api/v1/my-service/order` is not routed to `my-service` at all.

So, complete configuration for our example:

```yaml
routeConfigurations:
- gateway: public-gateway-service
  routes:
  - prefix: /api/v1/my-service/resource
    prefixRewrite: /resource
    cluster: "my-service||my-service||8080"
  - prefix: /api/v1/my-service/resource/{var1}/internal-api
    allowed: false
  - prefix: /api/v1/my-service/resource/{var1}/migrated-api
    prefixRewrite: /resource
    cluster: "another-service||another-service||8080"
  - prefix: /api/v1/my-service/resource
    headerMatchers:
    - name: ":method"
      exactMatch: "GET"
    prefixRewrite: /resource
    cluster: "another-service||another-service||8080"
  - prefix: /api/v1/my-service/resource/{var1}
    prefixRewrite: /resource/{var1}
    cluster: "my-service||my-service||8080"
  - prefix: /api/v1/my-service/resource/{var1}/internal-api/status
    prefixRewrite: /resource/{var1}/internal-api/status
    cluster: "my-service||my-service||8080"
  - prefix: /api/v1/my-service/order/{var1}/items
    prefixRewrite: /order/{var1}/items
    cluster: "my-service||my-service||8080"
- gateway: private-gateway-service
  routes:
  - prefix: /api/v1/my-service/resource
    prefixRewrite: /resource
    cluster: "my-service||my-service||8080"
  - prefix: /api/v1/my-service/resource/{var1}/migrated-api
    prefixRewrite: /resource
    cluster: "another-service||another-service||8080"
  - prefix: /api/v1/my-service/resource/{var1}/internal-api
    allowed: false
  - prefix: /api/v1/my-service/resource
    headerMatchers:
    - name: ":method"
      exactMatch: "GET"
    prefixRewrite: /resource
    cluster: "another-service||another-service||8080"
  - prefix: /api/v1/my-service/resource/{var1}
    prefixRewrite: /resource/{var1}
    cluster: "my-service||my-service||8080"
  - prefix: /api/v1/my-service/resource/{var1}/internal-api/status
    prefixRewrite: /resource/{var1}/internal-api/status
    cluster: "my-service||my-service||8080"
  - prefix: /api/v1/my-service/order/{var1}/items
    prefixRewrite: /order/{var1}/items
    cluster: "my-service||my-service||8080"
- gateway: internal-gateway
  routes:
  - prefix: /api/v1/my-service/resource
    prefixRewrite: /resource
    cluster: "my-service||my-service||8080"
  - prefix: /api/v1/my-service/resource/{var1}/migrated-api
    prefixRewrite: /resource
    cluster: "another-service||another-service||8080"
  - prefix: /api/v1/my-service/resource/{var1}/internal-api
    prefixRewrite: /resource/{var1}/internal-api
    cluster: "my-service||my-service||8080"
  - prefix: /api/v1/my-service/resource
    headerMatchers:
    - name: ":method"
      exactMatch: "GET"
    prefixRewrite: /resource
    cluster: "another-service||another-service||8080"
  - prefix: /api/v1/my-service/resource/{var1}
    prefixRewrite: /resource/{var1}
    cluster: "my-service||my-service||8080"
  - prefix: /api/v1/my-service/resource/{var1}/internal-api/status
    prefixRewrite: /resource/{var1}/internal-api/status
    cluster: "my-service||my-service||8080"
  - prefix: /api/v1/my-service/order/{var1}/items
    prefixRewrite: /order/{var1}/items
    cluster: "my-service||my-service||8080"
```

In legacy mesh `GET /api/v1/my-service/resource/123` goes to `my-service`: `/api/v1/my-service/resource/{var1}` is the longer match, and header matchers only break ties between matches of equal length. So the `:method` route only receives the bare collection path `/api/v1/my-service/resource` (and `/api/v1/my-service/resource/`).



## Istio Ambient Mesh Behavior



### Routes Ordering in Istio

In Istio Ambient Mesh routes are ordered in bit differently:

1. `Exact` match routes match first - from longest to shortest match.
2. `Prefix` match routes match only if not a single `Exact` route matched regardless of the match length (`Exact` will win even if it is shorter than `Prefix`). Amoung `Prefix` routes the longest match wins.
3. `Regex` match routes match only if not a single `Exact` or `Prefix` route matched regardless of the match length (`Prefix` will win even if it is shorter than `Regex`). Amoung `Regex` routes the longest match wins.



#### Evidence that Route Precedence Is Not Configurable

There is no way to tell Istio to treat regex matches with a higher or equal priority than prefix matches. The tier ranking is hardcoded in the Gateway API translation in `pilot/pkg/config/kube/gateway/conversion.go`:

```go
// getURIRank ranks a URI match type. Exact > Prefix > Regex
func getURIRank(match *istio.HTTPMatchRequest) int {
	if match.Uri == nil {
		return -1
	}
	switch match.Uri.MatchType.(type) {
	case *istio.StringMatch_Exact:
		return 3
	case *istio.StringMatch_Prefix:
		return 2
	case *istio.StringMatch_Regex:
		return 1
	}
	// should not happen
	return -1
}
```

`sortHTTPRoutes` compares this rank first, and only for equal ranks falls through to match length, then method, then header count, then query param count. There is no environment variable, annotation, `HTTPRoute` field or `MeshConfig` option that overrides it (`pilot/pkg/features/pilot.go` has no route ordering/precedence flag).

Upstream Gateway API does not offer a knob either. `HTTPRouteRule.Matches` in `apis/v1/httproute_types.go` defines precedence as Exact -> PathPrefix (largest number of characters) -> method -> header count -> query param count, and then states:

> Note: The precedence of RegularExpression path matches are implementation-specific.

There is no priority/weight/order field in the API - so regex precedence is entirely the implementation's choice, and Istio's choice is "lowest tier".

Two consequences worth keeping in mind:

- the `Matches` list order inside a single rule does **not** give us priority - Istio re-sorts everything globally across all rules attached to the parent;
- what *is* usable is the intra-tier tie-break: among regex matches, `getURILength` returns `len(match.Uri.GetRegex())`, i.e. the raw regex **source string** length.



### There Is No Regex Rewrite In Gateway API

In HTTPRoute resource there is no RegexRewrite rule for path, supported options only include:

1. PathRewrite - rewrites full path;
2. PrefixRewrite - rewrites matched prefix, so it requires that route uses Prefix match.



## Actual Problems

Actual differences in behavior that need to be adressed:


| Problem       | Descrition                                                                                                                     | Solution                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Regex Rewrite | We cannot rewrite matched regex, so migrated route will be routed to the backend that will respond with 404.                   | Replace this route with prefix (see [Option 1](#option-1-prefix-matches-everywhere---forbidden-routes-move-to-authorizationpolicy)). But after we do that, we need to check that there is no another original route in this gateway that we overlap and change its behavior. In case there is overlap with different behavior, single behavior should be choosen or fallback to `VirtualService`([Option 3](#option-3-virtualservice-on-the-waypoint)).                                                                                                                                     |
| Ordering      | Regex route will match only if no prefix route matched. If there is shorter prefix route with another behavior - we break BWC. | Istio route precedence is not configurable (see [Route Precedence Is Not Configurable](#route-precedence-is-not-configurable)). Keep every route in the prefix tier and express the forbidden ones as `AuthorizationPolicy` (see [Option 1](#option-1-prefix-matches-everywhere---forbidden-routes-move-to-authorizationpolicy)), move the conflicting prefix route into the regex tier (see [Option 2](#option-2-normalize-the-tier---convert-conflicting-prefix-routes-to-regex)), or switch the waypoint to `VirtualService` (see [Option 3](#option-3-virtualservice-on-the-waypoint)). |


By route behavior we mean: 

1. Route is forbidden (directResponse with code 404); or
2. Route action combination:
  a) route leads to the specific cluster; and
    b) route rewrites path; and
    c) route modificates (add/remove) request headers.



## Possible Solutions



### Option 1: Prefix Matches Everywhere - Forbidden Routes Move To AuthorizationPolicy - Choosen Option

Instead of fighting the tier ranking, we can stop using the regex tier at all. The option has two moving parts:

1. **Every route becomes a** `PathPrefix` **match.** A legacy prefix containing path variables is truncated at the first variable: `/api/v1/my-service/resource/{var1}/internal-api` -> `/api/v1/my-service/resource`. Because all routes then live in the same tier, the legacy "longest prefix wins" ordering is reproduced exactly by Istio's longest-`PathPrefix`-wins tie-break, and `ReplacePrefixMatch` stays available, so rewrites keep working and path variables survive - in the vast majority of our routes `prefixRewrite` only strips the gateway-facing part of the path, so everything after the first variable is byte-identical in `prefix` and `prefixRewrite`.
2. **Every** `allowed: false` **route becomes an** `AuthorizationPolicy` **with** `action: DENY`, using the `{*}` / `{**}` path template operators instead of a regex match. Authorization is enforced in the RBAC HTTP filter, which runs *before* route selection and before the router filter applies the rewrite, so the policy sees the original request path and the route tier ranking is irrelevant. `DENY` is also evaluated before any `ALLOW` policy, so it cannot be overridden.

For the example above, the first and the second route migrate cleanly:

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: my-public-routes
spec:
  parentRefs:
    - name: public-gateway-service
  rules:
    - matches:
        - path:
            type: PathPrefix
            value: "/api/v1/my-service/resource"
      filters:
        - type: URLRewrite
          urlRewrite:
            path:
              type: ReplacePrefixMatch
              replacePrefixMatch: "/resource"
      backendRefs:
        - name: my-service
          port: 8080
---
apiVersion: security.istio.io/v1
kind: AuthorizationPolicy
metadata:
  name: my-forbidden-public-routes
spec:
  targetRefs:
    - group: gateway.networking.k8s.io
      kind: Gateway
      name: public-gateway-service
  action: DENY
  rules:
    - to:
        - operation:
            ports: ["8080"]   # the Gateway listener port
            paths:
              - "/api/v1/my-service/resource/{*}/internal-api"
              - "/api/v1/my-service/resource/{*}/internal-api/{**}"
            notPaths:
              - "/api/v1/my-service/resource/{*}/internal-api/status"
              - "/api/v1/my-service/resource/{*}/internal-api/status/{**}"
```

`{*}` matches exactly one path segment and `{**}` matches zero or more, and `{**}` must be the last operator. Two entries are needed to reproduce legacy prefix semantics: `{**}` is preceded by a literal `/`, so `/.../internal-api/{**}` covers `/.../internal-api/` and everything below it, while the first entry covers the bare `/.../internal-api`.

Since the policy is attached to one specific `Gateway`, the per-gateway exposure model is preserved: the same path can be denied on `public-gateway-service` and left untouched on `internal-gateway`.

`notPaths` restores the legacy "longest match wins" precedence for allowed routes below the forbidden one (see limitations below). The sixth route of the example (`/api/v1/my-service/resource/{var1}/internal-api/status`) is then routed by the `/api/v1/my-service/resource` rule like any other path below the controller root.

Limitations:

1. **Routes that need both a variable and a distinct behavior are still broken.** The third route of the example (`/api/v1/my-service/resource/{var1}/migrated-api` -> `another-service`) truncates to `/api/v1/my-service/resource`, which is exactly the prefix of the first route, so the two rules collide. This option only works for a route with path variables when the route is forbidden, or when truncating at the first variable does not overlap another route with different behavior (different cluster, different rewrite or different header modifications). The residual conflicts have to fall back to [Option 3](#option-3-virtualservice-on-the-waypoint). This is the same overlap check as in the `Regex Rewrite` row of [Actual Problems](#actual-problems) - the point of moving forbidden routes to `AuthorizationPolicy` is that it removes the largest source of such overlaps.
2. **Header-matched controller root route takes over requests with path variables.** The fifth route of the example (`/api/v1/my-service/resource/{var1}` -> `my-service`) truncates to `/api/v1/my-service/resource` and gets the same behavior as the first route, so on its own it migrates cleanly. But the fourth route (`/api/v1/my-service/resource` with `:method: GET` -> `another-service`) now has exactly the same `PathPrefix`. Path lengths tie, and Gateway API picks the rule with the method match, so every `GET` below the controller root, including `GET /api/v1/my-service/resource/123`, goes to `another-service` instead of `my-service`. Truncating to `/api/v1/my-service/resource/` does not help, since Istio strips the trailing `/` from a `PathPrefix` value. Such a header-matched controller root route must be registered as `Exact` match on the root path and the root path with a trailing `/` - the only paths it received in legacy. `Exact` is ranked above every `PathPrefix`, so collection requests still reach `another-service`, and everything below the root falls through to the truncated prefix route. `ReplacePrefixMatch` is not allowed with `Exact` match, so the rewrite becomes `ReplaceFullPath`. Query parameters are not part of the path, so `GET /api/v1/my-service/resource?page=2` still matches:

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: my-public-routes
spec:
  parentRefs:
    - name: public-gateway-service
  rules:
    - matches:
        - path:
            type: Exact
            value: "/api/v1/my-service/resource"
          method: GET
      filters:
        - type: URLRewrite
          urlRewrite:
            path:
              type: ReplaceFullPath
              replaceFullPath: "/resource"
      backendRefs:
        - name: another-service
          port: 8080
    - matches:
        - path:
            type: Exact
            value: "/api/v1/my-service/resource/"
          method: GET
      filters:
        - type: URLRewrite
          urlRewrite:
            path:
              type: ReplaceFullPath
              replaceFullPath: "/resource/"
      backendRefs:
        - name: another-service
          port: 8080
```

3. **Forbidden route blocks longer allowed routes below it.** In legacy the forbidden route is a regular route, so a longer allowed route below it wins: `GET /api/v1/my-service/resource/123/internal-api/status` matches the sixth route of the example and goes to `my-service` on every gateway. `AuthorizationPolicy` is evaluated before route selection, so a `DENY` rule on `/api/v1/my-service/resource/{*}/internal-api/{**}` alone would block this request with 403 regardless of any longer route. Every allowed route which is longer than the forbidden one and lies below it must be added to `notPaths` of the same `DENY` rule - the bare path and the path followed by `/{**}`, with every path variable replaced by `{*}`. A request is denied only if it matches `paths` and does not match `notPaths`, and `notPaths` supports the same `{*}` / `{**}` path templates as `paths`. The typical case is a whole microservice root forbidden on `public-gateway-service` with only a few endpoints below it allowed - there every allowed endpoint ends up in `notPaths`. If the allowed route also has a `:method` header matcher, excluding its path from the `DENY` rule allows it for every method, so the other methods need a separate `DENY` rule with the same path and `notMethods`:

```yaml
  rules:
    - to:
        - operation:
            ports: ["8080"]
            paths:
              - "/api/v1/my-service/resource/{*}/internal-api"
              - "/api/v1/my-service/resource/{*}/internal-api/{**}"
            notPaths:
              - "/api/v1/my-service/resource/{*}/internal-api/status"
              - "/api/v1/my-service/resource/{*}/internal-api/status/{**}"
```

4. **Truncated route exposes paths which were not routed before.** A legacy route with path variables matches only its exact shape: the seventh route of the example exposes `/api/v1/my-service/order/{var1}/items` and nothing else, so `GET /api/v1/my-service/order/123` falls through to the gateway's own `/` route. After truncation to `/api/v1/my-service/order` the whole subtree below the controller is routed to `my-service`, including `GET /api/v1/my-service/order/123` and `DELETE /api/v1/my-service/order/123`, which may be `internal` endpoints of `my-service` never meant to be exposed on this gateway. If the route ends with a variable (`/api/v1/my-service/order/{var1}`), only the bare `/api/v1/my-service/order` and `/api/v1/my-service/order/` become exposed. No `allowed: false` route exists for these paths, so no `DENY` rule is generated from legacy configuration - the generator must create an additional `DENY` rule itself: `paths` are the truncated prefix and the truncated prefix followed by `/{**}`, `notPaths` are the original route and every other allowed route below the truncated prefix, in the same form as in the previous item. Such rule is needed only when no allowed route shorter than the truncated prefix covers it (the gateway's own `/` route does not count). Otherwise these paths were already exposed via the shorter route in legacy, and denying them would break it:

```yaml
  rules:
    - to:
        - operation:
            ports: ["8080"]
            paths:
              - "/api/v1/my-service/order"
              - "/api/v1/my-service/order/{**}"
            notPaths:
              - "/api/v1/my-service/order/{*}/items"
              - "/api/v1/my-service/order/{*}/items/{**}"
```

5. **The status code changes from 404 to 403.** Legacy `allowed: false` produced a `directResponse` with code 404; a denied request gets `403` with body `RBAC: access denied`. There is no way to make `AuthorizationPolicy` return 404, so a client that distinguishes the two sees a BWC break.
6. **Path normalization must be enabled.** The deny decision is path-based, so `%2F`, `..` and duplicate slashes become bypass vectors. `meshConfig.pathNormalization.normalization` must be at least `MERGE_SLASHES` (see [Authorization Policy Normalization](https://istio.io/latest/docs/ops/best-practices/security/#understand-path-normalization)).
7. `DENY` **policies should be scoped to a port.** For non-HTTP traffic all HTTP attributes are missing, and missing attributes are treated as matches in a `DENY` rule, so an unscoped policy denies more than intended.



### Option 2: Normalize The Tier - Convert Conflicting Prefix Routes To Regex

Since the tie-break inside the regex tier is longest-match-wins, we can restore legacy ordering by making sure conflicting routes live in the *same* tier: convert the shorter prefix route to a regex matcher as well (`/api/v4/tenant-manager/tenants` -> `/api/v4/tenant-manager/tenants(/.*)?`). This reproduces legacy Cloud-Core Service Mesh semantics exactly, because legacy ordering already treated a regex matcher as a prefix and compared lengths.

Limitations:

1. **No prefix rewrite.** `ReplacePrefixMatch` is only compatible with a `PathPrefix` match; using it together with a regex match makes Istio set `Accepted: False` on the whole route. Only `ReplaceFullPath` remains, which rewrites to a static path and therefore loses path variables (Istio compiles it to `uriRegexRewrite` with match `/.`*). So this option is applicable to routes that need **no** rewrite - in particular the `allowed: false` (404) routes - or a static rewrite.
2. **The tie-break metric is not the same as in legacy.** Legacy compared the prefix string containing `{tenantId}` (10 characters), while Istio compares the compiled regex where the same variable becomes `([^/]+)` (8 characters). Variable-heavy paths therefore shrink relative to literal-heavy ones, and the relative order of two routes can flip. Any generator using this option must verify (or pad/normalize the produced regexes) so that regex source length preserves the legacy ordering.



### Option 3: VirtualService On The Waypoint

`VirtualService` solves both the ordering problem and the regex rewrite problem:

- **Explicit ordering.** Istio does not re-sort `VirtualService` routes - `sortHTTPRoutes` exists only in the Gateway API conversion path. Routes are evaluated first-match-wins in the order they are declared, so we control priority directly and can emit routes in exactly the legacy order.
- **Regex rewrite is available.** `HTTPRewrite` in `istio/api networking/*/virtual_service.proto` has `uri_regex_rewrite` (`RegexRewrite`, field 3), which is a direct translation of the legacy envoy `regexpRewrite` including capture groups (`\1`, `\2`, ...).

Costs, per [Use Layer 7 features](https://istio.io/latest/docs/ambient/usage/l7-features/):

> Usage of VirtualService with the ambient data plane mode is considered Alpha.

> Mixing with Gateway API configuration is not supported, and will lead to undefined behavior.

So it is an all-or-nothing choice per waypoint (we cannot keep `HTTPRoute` for simple routes and add `VirtualService` only for the regex ones), and it means relying on an Alpha feature while `HTTPRoute` for waypoints is Beta.

### Option 4: EnvoyFilter To Reorder Routes - Not Viable

Rewriting the generated route table order with an `EnvoyFilter` doesn't seem to be possible. 

But adding regexRewrite to the routes can be achieved via `EnvoyFilter` by referencing route from `HTTPRoute` by its generated name. This solution is even more fragile then Option 3. While working, `EnvoyFilter` for ambient is not officially supported, and is actively discouraged by the maintainers.

### Summary

[Option 1](#option-1-prefix-matches-everywhere---forbidden-routes-move-to-authorizationpolicy---choosen-option) is the choosen option. Manual fallback to [Option 3](#option-3-virtualservice-on-the-waypoint) in very rare case when option 1 leads to having two allowed routes with different behavior and it is impossible to choose single universal behavior for them.


| Option                                                                            | Fixes ordering                                                 | Fixes regex rewrite                 | Maturity                                                                                   |
| --------------------------------------------------------------------------------- | -------------------------------------------------------------- | ----------------------------------- | ------------------------------------------------------------------------------------------ |
| `HTTPRoute` as-is                                                                 | no                                                             | no                                  | Beta                                                                                       |
| `HTTPRoute` with prefix matches only + `AuthorizationPolicy` for forbidden routes | yes, the regex tier is not used at all                         | yes, prefix rewrite stays available | Beta (`HTTPRoute`), Stable (`AuthorizationPolicy`), needs Istio >= 1.22 for path templates |
| `HTTPRoute` + convert conflicting prefix routes to regex                          | yes, but only for routes without a variable-preserving rewrite | no                                  | Beta                                                                                       |
| `VirtualService` on waypoint                                                      | yes                                                            | yes                                 | Alpha, cannot be mixed with Gateway API config                                             |
| `EnvoyFilter`                                                                     | -                                                              | -                                   | not supported for waypoints                                                                |


Sources verified against `istio/istio` and `kubernetes-sigs/gateway-api` `master` on 2026-08-18.